package com.earseo.story.service;

import com.earseo.story.entity.Locale;
import com.earseo.story.entity.Story;
import com.earseo.story.entity.StoryConcept;
import com.earseo.story.repository.StoryJdbcRepository;
import com.earseo.story.repository.StorySpotSummaryJdbcRepository;
import com.earseo.story.repository.StorySpotSummaryJdbcRepository.SpotSummarySaveRequest;
import com.earseo.story.service.OpenAiService.SpotSummaryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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

    private final OpenAiService openAiService;
    private final StoryJdbcRepository storyJDBCRepository;
    private final StorySpotSummaryJdbcRepository storySpotSummaryJDBCRepository;

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

        log.info("{} Summary Created", insertedSummaryCnt);
    }
}
