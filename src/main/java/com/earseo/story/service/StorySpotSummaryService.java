package com.earseo.story.service;

import com.earseo.story.dto.internal.StoryDocentRequest;
import com.earseo.story.dto.internal.StoryDocentResponse;
import com.earseo.story.dto.request.GetRouteListSpotRequest;
import com.earseo.story.dto.request.PathLineStringRequest;
import com.earseo.story.dto.request.PointRequest;
import com.earseo.story.dto.response.GetRouteListSpotResponse;
import com.earseo.story.dto.response.GetRouteSpotResponse;
import com.earseo.story.entity.Locale;
import com.earseo.story.entity.Story;
import com.earseo.story.entity.StoryConcept;
import com.earseo.story.entity.StorySpotSummary;
import com.earseo.story.repository.StoryJdbcRepository;
import com.earseo.story.repository.StorySpotSummaryJdbcRepository;
import com.earseo.story.repository.StorySpotSummaryJdbcRepository.SpotSummarySaveRequest;
import com.earseo.story.repository.StorySpotSummaryRepository;
import com.earseo.story.repository.projectionDto.StorySpotWithSummaryProjection;
import com.earseo.story.service.OpenAiService.SpotSummaryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorySpotSummaryService {
    @Value("${summary.hot-spot.threshold}")
    private int HOT_SPOT_THRESHOLD;
    @Value("${summary.story-summary.limit}")
    private int STORY_SUMMARY_LIMIT;
    @Value("${summary.story-summary.threshold}")
    private int STORY_SUMMARY_THRESHOLD;
    @Value("${distance.path-to-spot}")
    private int DISTANCE_PATH_TO_SPOT;

    private static final int SRID_WGS84 = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY =
        new GeometryFactory(new PrecisionModel(), SRID_WGS84);
    private static final Random SingletonRandom = new Random();
    private static final int INTERVAL_MINUTES = 3;

    private final OpenAiService openAiService;
    private final StoryJdbcRepository storyJDBCRepository;
    private final StorySpotSummaryJdbcRepository storySpotSummaryJDBCRepository;
    private final StorySpotSummaryRepository storySpotSummaryRepository;
    private final CoreFeignClient coreFeignClient;

    @Scheduled(cron = "${summary.schedule.cron}")
    public void createSummariesScheduled() {
        log.info("Starting summary creation");

        // 핫스팟 찾기
        List<Long> hotSpotIds = storyJDBCRepository.findHotStorySpot(HOT_SPOT_THRESHOLD);
        if (hotSpotIds.isEmpty()) {
            return;
        }

        // 이전 요약 대상 이야기 id
        // Map<StorySpotId, Map<StoryConcept, Set<storyId>>>
        Map<Long, Map<StoryConcept, Set<Long>>> lastSummarizedStoryIdSet =
            storySpotSummaryJDBCRepository.findAllLastSummarizedStoryIdSet(hotSpotIds);

        // 현재 요약 대상 이야기
        // Map<StorySpotId, Map<StoryConcept, Set<Story>>>
        Map<Long, Map<StoryConcept, Set<Story>>> currentSummarizedStoryIdSet =
            storyJDBCRepository.findAllHotStorySummarizeTargetStory(hotSpotIds, STORY_SUMMARY_LIMIT);

        // 요약 정보를 새로 만들어야 하는 이야기
        Map<Long, Map<StoryConcept, Set<Story>>> shouldSummarizeStory = new HashMap<>();
        long summarizeCnt = 0L;

        for (Long hotSpotId : hotSpotIds) {
            Map<StoryConcept, Set<Story>> currentStories =
                currentSummarizedStoryIdSet.getOrDefault(hotSpotId, Collections.emptyMap());

            for (StoryConcept storyConcept : StoryConcept.values()) {
                Set<Story> stories = currentStories.get(storyConcept);
                if (stories == null || stories.isEmpty()) continue;
                Set<Long> prevStoryIds = lastSummarizedStoryIdSet
                    .getOrDefault(hotSpotId, Collections.emptyMap())
                    .getOrDefault(storyConcept, Collections.emptySet());
                if (
                    stories.size() < STORY_SUMMARY_THRESHOLD
                    || stories.stream().map(Story::getId).collect(Collectors.toSet()).equals(prevStoryIds)
                ) continue;
                shouldSummarizeStory
                    .computeIfAbsent(hotSpotId, k -> new HashMap<>())
                    .computeIfAbsent(storyConcept, k -> new HashSet<>())
                    .addAll(stories);
                summarizeCnt++;
            }
        }

        if (summarizeCnt == 0) return;

        List<SpotSummarySaveRequest> spotSummarySaveRequests = new ArrayList<>();
        // 요약 생성
        for (Map.Entry<Long, Map<StoryConcept, Set<Story>>> storySpotEntry : shouldSummarizeStory.entrySet()) {
            for (Map.Entry<StoryConcept, Set<Story>> storyConceptEntry : storySpotEntry.getValue().entrySet()) {
                for (Locale locale : Locale.values()) {
                    SpotSummaryResult spotSummaryResult = openAiService.generateSummaryWithAI(
                        storyConceptEntry.getValue(),
                        storyConceptEntry.getKey(),
                        locale
                    );
                    SpotSummarySaveRequest spotSummarySaveRequest = SpotSummarySaveRequest
                        .toDto(
                            spotSummaryResult,
                            storySpotEntry.getKey(),
                            storyConceptEntry.getKey(),
                            locale,
                            storyConceptEntry.getValue()
                        );
                    spotSummarySaveRequests.add(spotSummarySaveRequest);
                }
            }
        }

        // 요약 저장
        long insertedSummaryCnt = storySpotSummaryJDBCRepository
            .batchUpsert(spotSummarySaveRequests);

        createDocentUrl();

        log.info("{} Summary Created", insertedSummaryCnt);
    }

    @Transactional
    public void createDocentUrl() {
        //최근 수정된 요약 조회
        List<StorySpotSummary> storySpotSummaries = storySpotSummaryRepository.findAllByUpdatedAt(INTERVAL_MINUTES);
        if (storySpotSummaries.isEmpty()) return;
        List<StoryDocentRequest> storyDocentRequests = storySpotSummaries.stream()
            .map(
                item -> new StoryDocentRequest(
                    item.getId(),
                    item.getSummary(),
                    item.getLocale().name()
                )
            )
            .toList();
        List<StoryDocentResponse> storyDocentResponses = coreFeignClient.getStoryDocent(storyDocentRequests);
        for (int i = 0; i < storyDocentResponses.size(); i++) {
            StoryDocentResponse storyDocentResponse = storyDocentResponses.get(i);
            storySpotSummaries.get(i).updateDocent(storyDocentResponse.docentUrl(), storyDocentResponse.docentScript());
        }
    }

    @Transactional(readOnly = true)
    public GetRouteListSpotResponse getPathsSpotList(GetRouteListSpotRequest request) {
        Integer meters = request.meters() != null ? request.meters() : DISTANCE_PATH_TO_SPOT;

        List<List<GetRouteSpotResponse>> spotList = request.paths().stream()
            .map(path -> processPath(path, meters, request.storyConcept()))
            .toList();

        return GetRouteListSpotResponse.toDto(spotList);
    }

    private List<GetRouteSpotResponse> processPath(
        PathLineStringRequest pathRequest,
        Integer meters,
        StoryConcept requestedConcept
    ) {
        // 유효성 검증
        if (pathRequest.lineString() == null || pathRequest.lineString().size() < 2 || pathRequest.amount() <= 0) {
            return List.of();
        }

        // LineString 생성
        LineString lineString = buildJTSLineString(pathRequest.lineString());

        // 컨셉이 null이면 가공
        StoryConcept selectedConcept = requestedConcept != null
            ? requestedConcept
            : getRandomConcept();

        // 한방 쿼리
        List<StorySpotWithSummaryProjection> spotsWithSummaries =
            storySpotSummaryRepository.findSpotsNearPathWithSummaries(
                lineString,
                meters,
                Locale.KO.name(),
                selectedConcept.name(),
                pathRequest.amount()
            );

        if (spotsWithSummaries.isEmpty()) {
            return List.of();
        }

        // 가장 가까운것 반환
        return spotsWithSummaries.stream().map(GetRouteSpotResponse::toDto).toList();
    }

    private StoryConcept getRandomConcept() {
        StoryConcept[] concepts = StoryConcept.values();
        return concepts[new Random().nextInt(concepts.length)];
    }

    private LineString buildJTSLineString(List<PointRequest> points) {
        Coordinate[] coordinates = points.stream()
            .map(point -> new Coordinate(point.longitude(), point.latitude()))
            .toArray(Coordinate[]::new);

        LineString lineString = GEOMETRY_FACTORY.createLineString(coordinates);
        lineString.setSRID(SRID_WGS84);
        return lineString;
    }
}
