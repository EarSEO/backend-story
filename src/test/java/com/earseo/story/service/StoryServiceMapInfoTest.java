package com.earseo.story.service;

import com.earseo.story.common.exception.BaseException;
import com.earseo.story.dto.response.MapSpotInfoList;
import com.earseo.story.entity.StorySpot;
import com.earseo.story.repository.StorySpotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("지도 조회 테스트")
class StoryServiceMapInfoTest {

    @Mock
    private StorySpotRepository storySpotRepository;

    @InjectMocks
    private StoryService storyService;

    private static final GeometryFactory GEOMETRY_FACTORY =
        new GeometryFactory(new PrecisionModel(), 4326);

    @Test
    @DisplayName("사각형 영역 내의 이야기 스팟 목록을 조회한다")
    void getRectangleMapInfoList_success() {
        // given
        Double minLongitude = 126.9;
        Double minLatitude = 37.5;
        Double maxLongitude = 127.1;
        Double maxLatitude = 37.6;

        StorySpot spot1 = createStorySpot(1L, 126.95, 37.55);
        StorySpot spot2 = createStorySpot(2L, 127.0, 37.56);
        StorySpot spot3 = createStorySpot(3L, 127.05, 37.57);

        List<StorySpot> mockSpots = List.of(spot1, spot2, spot3);

        given(storySpotRepository.findByBoundingBox(minLongitude, minLatitude, maxLongitude, maxLatitude))
            .willReturn(mockSpots);

        // when
        MapSpotInfoList result = storyService.getRectangleMapInfoList(
            minLongitude, minLatitude, maxLongitude, maxLatitude
        );

        // then
        assertThat(result.storySpots()).hasSize(3);
        assertThat(result.storySpots().get(0).longitude()).isEqualTo(126.95);
        assertThat(result.storySpots().get(0).latitude()).isEqualTo(37.55);
        assertThat(result.storySpots().get(0).storySpotId()).isEqualTo(1L);
        assertThat(result.storySpots().get(1).longitude()).isEqualTo(127.0);
        assertThat(result.storySpots().get(1).latitude()).isEqualTo(37.56);
        assertThat(result.storySpots().get(2).longitude()).isEqualTo(127.05);
        assertThat(result.storySpots().get(2).latitude()).isEqualTo(37.57);
        assertThat(result.storySpots().get(2).storySpotId()).isEqualTo(3L);

        then(storySpotRepository).should(times(1))
            .findByBoundingBox(minLongitude, minLatitude, maxLongitude, maxLatitude);
    }

    @Test
    @DisplayName("영역 내에 스팟이 없는 경우 빈 목록을 반환한다")
    void getRectangleMapInfoList_emptyResult() {
        // given
        Double minLongitude = 126.9;
        Double minLatitude = 37.5;
        Double maxLongitude = 127.1;
        Double maxLatitude = 37.6;

        given(storySpotRepository.findByBoundingBox(minLongitude, minLatitude, maxLongitude, maxLatitude))
            .willReturn(List.of());

        // when
        MapSpotInfoList result = storyService.getRectangleMapInfoList(
            minLongitude, minLatitude, maxLongitude, maxLatitude
        );

        // then
        assertThat(result.storySpots()).isEmpty();
        then(storySpotRepository).should(times(1))
            .findByBoundingBox(minLongitude, minLatitude, maxLongitude, maxLatitude);
    }

    @Test
    @DisplayName("최소 경도가 최대 경도보다 크거나 같으면 예외가 발생한다")
    void getRectangleMapInfoList_invalidLongitudeRange() {
        // given
        Double minLongitude = 127.1;
        Double minLatitude = 37.5;
        Double maxLongitude = 126.9;
        Double maxLatitude = 37.6;

        // when & then
        assertThatThrownBy(() -> storyService.getRectangleMapInfoList(
            minLongitude, minLatitude, maxLongitude, maxLatitude
        )).isInstanceOf(BaseException.class);

        then(storySpotRepository).should(never())
            .findByBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("최소 위도가 최대 위도보다 크거나 같으면 예외가 발생한다")
    void getRectangleMapInfoList_invalidLatitudeRange() {
        // given
        Double minLongitude = 126.9;
        Double minLatitude = 37.6;
        Double maxLongitude = 127.1;
        Double maxLatitude = 37.5;

        // when & then
        assertThatThrownBy(() -> storyService.getRectangleMapInfoList(
            minLongitude, minLatitude, maxLongitude, maxLatitude
        )).isInstanceOf(BaseException.class);

        then(storySpotRepository).should(never())
            .findByBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("중심점 반경 내의 이야기 스팟 목록을 조회한다")
    void getCircleMapInfo_success() {
        // given
        Double meters = 1000.0;
        Double longitude = 126.9780;
        Double latitude = 37.5665;

        StorySpot spot1 = createStorySpot(1L, 126.9790, 37.5670);
        StorySpot spot2 = createStorySpot(2L, 126.9800, 37.5675);
        StorySpot spot3 = createStorySpot(3L, 126.9770, 37.5660);

        List<StorySpot> mockSpots = List.of(spot1, spot2, spot3);

        given(storySpotRepository.findByRadius(longitude, latitude, meters))
            .willReturn(mockSpots);

        // when
        MapSpotInfoList result = storyService.getCircleMapInfo(meters, longitude, latitude);

        // then
        assertThat(result.storySpots()).hasSize(3);
        assertThat(result.storySpots().get(0).longitude()).isEqualTo(126.9790);
        assertThat(result.storySpots().get(0).latitude()).isEqualTo(37.5670);
        assertThat(result.storySpots().get(0).storySpotId()).isEqualTo(1L);

        then(storySpotRepository).should(times(1))
            .findByRadius(longitude, latitude, meters);
    }

    @Test
    @DisplayName("반경 내에 스팟이 없는 경우 빈 목록을 반환한다")
    void getCircleMapInfo_emptyResult() {
        // given
        Double meters = 1000.0;
        Double longitude = 126.9780;
        Double latitude = 37.5665;

        given(storySpotRepository.findByRadius(longitude, latitude, meters))
            .willReturn(List.of());

        // when
        MapSpotInfoList result = storyService.getCircleMapInfo(meters, longitude, latitude);

        // then
        assertThat(result.storySpots()).isEmpty();
        then(storySpotRepository).should(times(1))
            .findByRadius(longitude, latitude, meters);
    }

    private StorySpot createStorySpot(Long id, Double longitude, Double latitude) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));

        return StorySpot.builder()
            .id(id)
            .center(point)
            .geohash("geohash" + id)
            .build();
    }
}
