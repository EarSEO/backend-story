package com.earseo.story.service;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import com.earseo.story.common.exception.BaseException;
import com.earseo.story.common.exception.StorySpotError;
import com.earseo.story.dto.request.CreateRequest;
import com.earseo.story.dto.request.UpdateStoryRequest;
import com.earseo.story.dto.response.*;
import com.earseo.story.entity.*;
import com.earseo.story.entity.Locale;
import com.earseo.story.repository.*;
import com.earseo.story.repository.projectionDto.SearchSpotProjection;
import com.earseo.story.repository.projectionDto.StorySpotWithDistanceProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

import static com.earseo.story.common.exception.StoryError.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {
    private static final int GEOHASH_PRECISION = 9;
    private static final int SRID_WGS84 = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), SRID_WGS84);
    private static final String STORY_S3_PATH_FORMAT = "story/%s/%d";
    private static final int STORY_SPOT_CANDIDATE_TITLE_LIMIT = 4;

    private final StorySpotRepository storySpotRepository;
    private final StoryAuthorRepository storyAuthorRepository;
    private final StoryRepository storyRepository;
    private final S3Service s3Service;
    private final StoryImageRepository storyImageRepository;
    private final StoryTitleRepository storyTitleRepository;
    private final SpotTitleAggregateRepository spotTitleAggregateRepository;
    private final StorySpotSummaryRepository storySpotSummaryRepository;
    private final StoryLikeRepository storyLikeRepository;
    private final LikeService likeService;

    @Transactional
    public CreateResponse createStory(Long memberId, CreateRequest body, List<MultipartFile> images) {
        // 사용자 검증
        if (!memberId.equals(body.authorId())) throw new BaseException(ITS_NOT_YOU);
        StoryAuthor storyAuthor = storyAuthorRepository.findById(body.authorId())
                .orElseGet(() ->
                        storyAuthorRepository.save(
                                StoryAuthor.builder()
                                        .id(body.authorId())
                                        .nickname(body.authorName())
                                        .profileUrl(body.authorProfileUrl())
                                        .updatedAt(body.authorProfileUpdatedAt())
                                        .build()
                        )
                );
        // 서버의 정보보다 새로운 정보가 있고, 실제로 변경사항이 있을 때만 업데이트
        if (storyAuthor.getUpdatedAt().isBefore(body.authorProfileUpdatedAt()) &&
                (!Objects.equals(storyAuthor.getNickname(), body.authorName()) ||
                        !Objects.equals(storyAuthor.getProfileUrl(), body.authorProfileUrl()))) {
            storyAuthor.updateAuthor(body.authorName(), body.authorProfileUrl(), body.authorProfileUpdatedAt());
        }
        // GeoHash 처리
        String spotGeoHash = getGeoHash(body.longitude(), body.latitude());
        Point centerPoint = getGeohashCenterPoint(body.longitude(), body.latitude());
        Point storyPoint = createPoint(body.longitude(), body.latitude());
        StorySpot geohashedSpot = storySpotRepository.findByGeohash(spotGeoHash)
                .orElseGet(() ->
                        storySpotRepository.save(
                                StorySpot.builder()
                                        .geohash(spotGeoHash)
                                        .center(centerPoint)
                                        .build()
                        )
                );
        // 이야기 타이틀 조회
        StoryTitle storyTitle = storyTitleRepository.findByTitle(body.title())
                .orElseGet(() ->
                        storyTitleRepository.save(
                                StoryTitle.builder()
                                        .title(body.title())
                                        .build()
                        )
                );
        // 이야기 저장
        Story story = storyRepository.save(Story.builder()
                .storySpot(geohashedSpot)
                .storyAuthor(storyAuthor)
                .point(storyPoint)
                .storyTitle(storyTitle)
                .content(body.content())
                .locale(body.locale())
                .storyConcept(body.storyConcept())
                .build());
        // 제목 집계 테이블 최신화
        spotTitleAggregateRepository.incrementOrCreate(geohashedSpot.getId(), storyTitle.getId());

        if (images != null && !images.isEmpty()) {
            List<StoryImage> storyImages = getImageList(images, spotGeoHash, story);
            storyImageRepository.saveAll(storyImages);
        }

        return CreateResponse.toDto(story);
    }

    @Transactional
    public CreateResponse saveImages(List<MultipartFile> images,Long memberId, Long storyId){
        Story story = storyRepository.findById(storyId).orElseThrow(()-> new BaseException(STORY_NOT_FOUND));
        if(story.getStoryAuthor().getId() != memberId) throw new BaseException(ITS_NOT_YOU);

        String spotGeoHash = story.getStorySpot().getGeohash();
        // 파일 저장
        if (images != null && !images.isEmpty()) {
            List<StoryImage> storyImages = getImageList(images, spotGeoHash, story);
            storyImageRepository.saveAll(storyImages);
        }
        return CreateResponse.toDto(story);
    }

    /**
     * Geohash 값 계산 메서드
     */
    private static String getGeoHash(double longitude, double latitude) {
        return GeoHash.withCharacterPrecision(latitude, longitude, GEOHASH_PRECISION).toBase32();
    }

    /**
     * Point 생성 메서드
     */
    private Point createPoint(double longitude, double latitude) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(SRID_WGS84);
        return point;
    }

    /**
     * Geohash 중심 좌표 계산 메서드
     */
    private Point getGeohashCenterPoint(double longitude, double latitude) {
        GeoHash geoHashObject = GeoHash.withCharacterPrecision(latitude, longitude, GEOHASH_PRECISION);
        WGS84Point centerWGS84 = geoHashObject.getBoundingBoxCenter();
        return createPoint(centerWGS84.getLongitude(), centerWGS84.getLatitude());
    }

    /**
     * 이미지 업로드, StoryImage 엔티티 생성 메서드
     */
    private List<StoryImage> getImageList(List<MultipartFile> images, String spotGeoHash, Story story) {
        List<StoryImage> storyImages = new ArrayList<>();
        for (MultipartFile image : images) {
            if (image.isEmpty()) {
                continue;
            }
            try {
                // S3에 파일 업로드
                String imageUrl = s3Service.uploadFile(image, String.format(STORY_S3_PATH_FORMAT, spotGeoHash, story.getId()));
                // StoryImage 엔티티 생성
                StoryImage storyImage = StoryImage.builder()
                        .story(story)
                        .imageUrl(imageUrl)
                        .build();
                storyImages.add(storyImage);
            } catch (Exception e) {
                // 업로드 실패 시 이미 업로드된 이미지들 롤백
                rollbackUploadedImages(storyImages);
                throw new BaseException(STORY_IMAGE_UPLOAD_FAILED);
            }
        }
        return storyImages;
    }

    private void rollbackUploadedImages(List<StoryImage> uploadedImages) {
        for (StoryImage storyImage : uploadedImages) {
            try {
                s3Service.deleteFile(storyImage.getImageUrl());
            } catch (Exception e) {
                log.error("이미지 업로드 롤백 실패", e);
            }
        }
    }

    public SpotTitleListResponse getSpotTitleList(Long storySpotId) {
        return SpotTitleListResponse.toDto(spotTitleAggregateRepository.findTopTitleBySpotId(storySpotId, Pageable.ofSize(STORY_SPOT_CANDIDATE_TITLE_LIMIT)));
    }

    public LocationSpotBriefInfoResponse getLocationSpotBriefInfo(Double longitude, Double latitude) {
        String geoHash = getGeoHash(longitude, latitude);
        Optional<StorySpot> storySpot = storySpotRepository.findByGeohash(geoHash);
        if (storySpot.isEmpty()) {
            return LocationSpotBriefInfoResponse.toDto(null, List.of());
        }
        return LocationSpotBriefInfoResponse.toDto(
                storySpot.get().getId(),
                spotTitleAggregateRepository.findTopTitleBySpotId(storySpot.get().getId(), Pageable.ofSize(STORY_SPOT_CANDIDATE_TITLE_LIMIT))
        );
    }

    public MapSpotInfoList getRectangleMapInfoList(
            Double minLongitude, Double minLatitude,
            Double maxLongitude, Double maxLatitude
    ) {
        if (minLongitude >= maxLongitude || minLatitude >= maxLatitude) {
            throw new BaseException(INVALID_COORDINATE_RANGE);
        }

        List<StorySpot> spots = storySpotRepository.findByBoundingBox(
                minLongitude, minLatitude, maxLongitude, maxLatitude
        );
        return MapSpotInfoList.toDto(spots);
    }

    public MapSpotInfoList getCircleMapInfo(Double meters, Double longitude, Double latitude) {
        List<StorySpot> spots = storySpotRepository.findByRadius(longitude, latitude, meters);
        return MapSpotInfoList.toDto(spots);
    }

    @Transactional(readOnly = true)
    public SearchSpotInfoList searchTitleRectangle(
            String keyword,
            Double longitude, Double latitude,
            Double minLongitude, Double minLatitude,
            Double maxLongitude, Double maxLatitude,
            int limit
    ) {
        if (minLongitude >= maxLongitude || minLatitude >= maxLatitude) {
            throw new BaseException(INVALID_COORDINATE_RANGE);
        }
        List<SearchSpotProjection> responses = spotTitleAggregateRepository.searchByTitleKeywordRectangle(
                keyword,
                longitude,
                latitude,
                minLongitude,
                minLatitude,
                maxLongitude,
                maxLatitude,
                limit
        );
        return SearchSpotInfoList.toDto(responses);
    }

    @Transactional(readOnly = true)
    public MyStoryListResponse getMyStories(Long memberId, Long lastStoryId, int size) {
        // size + 1개 조회해서 hasNext 판단
        Pageable pageable = PageRequest.of(0, size + 1);

        List<Story> stories;
        if (lastStoryId == null) {
            // 첫 조회
            stories = storyRepository.findByStoryAuthorIdOrderByIdDesc(memberId, pageable);
        } else {
            // 이후 조회
            stories = storyRepository.findByStoryAuthorIdAndIdLessThanOrderByIdDesc(memberId, lastStoryId, pageable);
        }

        // hasNext 판단
        boolean hasNext = stories.size() > size;
        if (hasNext) {
            stories = stories.subList(0, size); // 실제 size만큼만 반환
        }

        // 이미지 조회 (N+1 방지)
        List<Long> storyIds = stories.stream()
                .map(Story::getId)
                .toList();

        Map<Long, List<String>> imageUrlMap = Map.of();
        if (!storyIds.isEmpty()) {
            List<StoryImage> storyImages = storyImageRepository.findByStoryIdIn(storyIds);
            imageUrlMap = storyImages.stream()
                    .collect(Collectors.groupingBy(
                            si -> si.getStory().getId(),
                            Collectors.mapping(StoryImage::getImageUrl, Collectors.toList())
                    ));
        }

        return MyStoryListResponse.toDto(stories, imageUrlMap, hasNext);
    }

    @Transactional
    public ToggleLikeResponse toggleLike(Long storyId, Long memberId) {
        boolean isLiked = likeService.toggleLike(storyId, memberId);
        Long likeCount = likeService.getLikeCount(storyId);
        return new ToggleLikeResponse(isLiked, likeCount);
    }

    @Transactional(readOnly = true)
    public MyStoryListResponse getLikedStories(Long memberId, Long lastStoryLikeId, int size) {
        Pageable pageable = PageRequest.of(0, size + 1);

        List<StoryLike> storyLikes;
        if (lastStoryLikeId == null) {
            storyLikes = storyLikeRepository.findByMemberIdOrderByIdDesc(memberId, pageable);
        } else {
            storyLikes = storyLikeRepository.findByMemberIdAndIdLessThanOrderByIdDesc(memberId, lastStoryLikeId, pageable);
        }

        boolean hasNext = storyLikes.size() > size;
        if (hasNext) {
            storyLikes = storyLikes.subList(0, size);
        }

        List<Story> stories = storyLikes.stream()
                .map(StoryLike::getStory)
                .toList();

        // 이미지 조회
        List<Long> storyIds = stories.stream()
                .map(Story::getId)
                .toList();

        final Map<Long, List<String>> imageUrlMap;
        if (!storyIds.isEmpty()) {
            List<StoryImage> storyImages = storyImageRepository.findByStoryIdIn(storyIds);
            imageUrlMap = storyImages.stream()
                    .collect(Collectors.groupingBy(
                            si -> si.getStory().getId(),
                            Collectors.mapping(StoryImage::getImageUrl, Collectors.toList())
                    ));
        } else {
            imageUrlMap = Map.of();
        }

        Long lastId = storyLikes.isEmpty() ? null : storyLikes.get(storyLikes.size() - 1).getId();

        return new MyStoryListResponse(
                stories.stream()
                        .map(story -> MyStoryResponse.toDto(story, imageUrlMap.getOrDefault(story.getId(), List.of())))
                        .toList(),
                hasNext,
                lastId
        );
    }

    @Transactional
    public UpdateStoryResponse updateStory(Long storyId, Long memberId, UpdateStoryRequest request, List<MultipartFile> newImages) {
        // Story 조회 및 작성자 검증
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new BaseException(STORY_NOT_FOUND));

        if (!story.getStoryAuthor().getId().equals(memberId)) {
            throw new BaseException(STORY_NOT_OWNER);
        }

        // 제목 변경 처리
        if (request.title() != null && !request.title().equals(story.getStoryTitle().getTitle())) {
            // 기존 제목 카운트 감소
            spotTitleAggregateRepository.decrementOrDelete(
                    story.getStorySpot().getId(),
                    story.getStoryTitle().getId()
            );

            // 새 제목 조회 또는 생성
            StoryTitle newTitle = storyTitleRepository.findByTitle(request.title())
                    .orElseGet(() -> storyTitleRepository.save(
                            StoryTitle.builder()
                                    .title(request.title())
                                    .build()
                    ));

            // 새 제목 카운트 증가
            spotTitleAggregateRepository.incrementOrCreate(
                    story.getStorySpot().getId(),
                    newTitle.getId()
            );

        }

        // 내용/컨셉 변경
        String newContent = request.content() != null ? request.content() : story.getContent();
        StoryConcept newConcept = request.storyConcept() != null ? request.storyConcept() : story.getStoryConcept();

        // 이미지 처리
        List<StoryImage> existingImages = storyImageRepository.findByStoryId(storyId);
        List<String> keepUrls = request.keepImageUrls() != null ? request.keepImageUrls() : List.of();

        // 삭제할 이미지 S3에서 제거
        existingImages.stream()
                .filter(img -> !keepUrls.contains(img.getImageUrl()))
                .forEach(img -> {
                    try {
                        s3Service.deleteFile(img.getImageUrl());
                    } catch (Exception e) {
                        log.error("이미지 삭제 실패: {}", img.getImageUrl(), e);
                    }
                });

        // DB에서 삭제
        if (!keepUrls.isEmpty()) {
            storyImageRepository.deleteByStoryIdAndImageUrlNotIn(storyId, keepUrls);
        } else {
            // keepUrls가 비어있으면 모든 이미지 삭제
            storyImageRepository.deleteAll(existingImages);
        }

        // 새 이미지 업로드 및 저장
        int currentImageCount = keepUrls.size();
        if (newImages != null && !newImages.isEmpty()) {
            if (currentImageCount + newImages.size() > 3) {
                throw new BaseException(STORY_IMAGE_TOO_MANY);
            }

            List<StoryImage> newStoryImages = getImageList(
                    newImages,
                    story.getStorySpot().getGeohash(),
                    story
            );
            storyImageRepository.saveAll(newStoryImages);
        }

        // Story 엔티티 업데이트
        StoryTitle titleToUpdate = null;
        if (request.title() != null && !request.title().equals(story.getStoryTitle().getTitle())) {
            titleToUpdate = storyTitleRepository.findByTitle(request.title()).orElseThrow();
        }
        story.updateStory(titleToUpdate, newContent, newConcept);

        return new UpdateStoryResponse(story.getId(), story.getUpdatedAt());
    }

    public SpotTotalInfoResponse getSpotTotalInfo(
            Long storySpotId,
            Double longitude,
            Double latitude,
            Locale locale,
            Pageable pageable
    ) {
        // 스팟 정보
        StorySpotWithDistanceProjection storySpot = storySpotRepository.findByIdWithDistance(storySpotId, longitude, latitude)
                .orElseThrow(() -> new BaseException(StorySpotError.STORY_SPOT_NOT_FOUND));

        // 상위 4개 제목
        List<SpotTitleAggregate> topTitles = spotTitleAggregateRepository.findTopTitleBySpotId(
                storySpotId,
                PageRequest.ofSize(STORY_SPOT_CANDIDATE_TITLE_LIMIT)
        );

        // 이야기 목록
        Slice<Story> storyPage = storyRepository.findByStorySpotIdAndLocale(
                storySpotId, pageable
        );

        // 이미지 조회
        List<Long> storyIds = storyPage.getContent().stream()
                .map(Story::getId)
                .toList();
        List<StoryImage> storyImages = storyImageRepository.findByStoryIdIn(storyIds);

        Map<Long, List<String>> imageUrlsMap = storyImages.stream()
                .collect(Collectors.groupingBy(
                        image -> image.getStory().getId(),
                        Collectors.mapping(StoryImage::getImageUrl, Collectors.toList())
                ));

        // 요약 목록
        List<StorySpotSummary> summaries = storySpotSummaryRepository
                .findByStorySpotIdAndLocale(storySpotId, locale);

        return SpotTotalInfoResponse.toDto(storySpot, topTitles, storyPage, imageUrlsMap, summaries);
    }

    public SpotStoryPageResponse getSpotStorieSlice(Long storySpotId, Pageable pageable) {
        // 이야기 목록
        Slice<Story> storyPage = storyRepository.findByStorySpotIdAndLocale(storySpotId, pageable);

        // 이미지 조회
        List<Long> storyIds = storyPage.getContent().stream()
                .map(Story::getId)
                .toList();
        List<StoryImage> storyImages = storyImageRepository.findByStoryIdIn(storyIds);

        Map<Long, List<String>> imageUrlsMap = storyImages.stream()
                .collect(Collectors.groupingBy(
                        image -> image.getStory().getId(),
                        Collectors.mapping(StoryImage::getImageUrl, Collectors.toList())
                ));
        return SpotStoryPageResponse.toDto(storyPage, imageUrlsMap);
    }

    public SpotStoryPageResponse getStoriesRectangleMapInfoList(
            Double minLongitude,
            Double minLatitude,
            Double maxLongitude,
            Double maxLatitude,
            Pageable pageable
    ) {
        if (minLongitude >= maxLongitude || minLatitude >= maxLatitude) {
            throw new BaseException(INVALID_COORDINATE_RANGE);
        }

        List<Long> storySpotIdList = storySpotRepository.findByBoundingBox(
                minLongitude, minLatitude, maxLongitude, maxLatitude
        ).stream().map(StorySpot::getId).toList();

        // 이야기 목록
        Slice<Story> storyPage = storyRepository.findByStorySpotIdList(storySpotIdList, pageable);

        // 이미지 조회
        List<Long> storyIds = storyPage.getContent().stream()
                .map(Story::getId)
                .toList();
        List<StoryImage> storyImages = storyImageRepository.findByStoryIdIn(storyIds);

        Map<Long, List<String>> imageUrlsMap = storyImages.stream()
                .collect(Collectors.groupingBy(
                        image -> image.getStory().getId(),
                        Collectors.mapping(StoryImage::getImageUrl, Collectors.toList())
                ));
        return SpotStoryPageResponse.toDto(storyPage, imageUrlsMap);
    }
}
