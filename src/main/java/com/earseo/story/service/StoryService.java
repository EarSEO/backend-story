package com.earseo.story.service;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import com.earseo.story.common.exception.BaseException;
import com.earseo.story.dto.request.CreateRequest;
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
import static com.earseo.story.common.exception.StorySpotError.STORY_SPOT_NOT_FOUND;

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

    public SpotTotalInfoResponse getSpotTotalInfo(
        Long storySpotId,
        Double longitude,
        Double latitude,
        Locale locale,
        Pageable pageable
    ) {
        // 스팟 정보
        StorySpotWithDistanceProjection storySpot = storySpotRepository.findByIdWithDistance(storySpotId, longitude, latitude)
            .orElseThrow(() -> new BaseException(STORY_SPOT_NOT_FOUND));

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
}
