package com.earseo.story.service;

import com.earseo.story.dto.response.SpotTitleListResponse;
import com.earseo.story.entity.SpotTitleAggregate;
import com.earseo.story.entity.StorySpot;
import com.earseo.story.entity.StoryTitle;
import com.earseo.story.repository.SpotTitleAggregateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("이야기 스팟 제목 조회 테스트")
class StorySpotTitleListTest {

    @Mock
    private SpotTitleAggregateRepository spotTitleAggregateRepository;

    @InjectMocks
    private StoryService storyService;

    @Test
    @DisplayName("이야기 스팟의 상위 이야기 제목 목록을 조회한다")
    void getSpotTitleList_success() {
        // given
        Long storySpotId = 1L;

        StorySpot storySpot = StorySpot.builder()
            .id(storySpotId)
            .build();

        StoryTitle title1 = StoryTitle.builder()
            .id(1L)
            .title("서울 여행")
            .build();

        StoryTitle title2 = StoryTitle.builder()
            .id(2L)
            .title("맛집 투어")
            .build();

        StoryTitle title3 = StoryTitle.builder()
            .id(3L)
            .title("카페 탐방")
            .build();

        SpotTitleAggregate aggregate1 = SpotTitleAggregate.builder()
            .id(1L)
            .storySpot(storySpot)
            .storyTitle(title1)
            .storyCount(10L)
            .build();

        SpotTitleAggregate aggregate2 = SpotTitleAggregate.builder()
            .id(2L)
            .storySpot(storySpot)
            .storyTitle(title2)
            .storyCount(8L)
            .build();

        SpotTitleAggregate aggregate3 = SpotTitleAggregate.builder()
            .id(3L)
            .storySpot(storySpot)
            .storyTitle(title3)
            .storyCount(5L)
            .build();

        List<SpotTitleAggregate> mockAggregates = List.of(aggregate1, aggregate2, aggregate3);

        given(spotTitleAggregateRepository.findTop4TitleBySpotId(eq(storySpotId), any(Pageable.class)))
            .willReturn(mockAggregates);

        // when
        SpotTitleListResponse response = storyService.getSpotTitleList(storySpotId);

        // then
        assertThat(response.titles()).hasSize(3);
        assertThat(response.titles()).containsExactly("서울 여행", "맛집 투어", "카페 탐방");
        then(spotTitleAggregateRepository).should(times(1))
            .findTop4TitleBySpotId(eq(storySpotId), any(Pageable.class));
    }

    @Test
    @DisplayName("이야기 스팟에 이야기 제목이 없는 경우 빈 목록을 반환한다")
    void getSpotTitleList_emptyList() {
        // given
        Long storySpotId = 999L;
        List<SpotTitleAggregate> emptyList = List.of();

        given(spotTitleAggregateRepository.findTop4TitleBySpotId(eq(storySpotId), any(Pageable.class)))
            .willReturn(emptyList);

        // when
        SpotTitleListResponse response = storyService.getSpotTitleList(storySpotId);

        // then
        assertThat(response.titles()).isEmpty();
        then(spotTitleAggregateRepository).should(times(1))
            .findTop4TitleBySpotId(eq(storySpotId), any(Pageable.class));
    }

    @Test
    @DisplayName("이야기 스팟에 4개 이상의 이야기 제목이 있는 경우 상위 4개만 반환한다")
    void getSpotTitleList_moreThanFour() {
        // given
        Long storySpotId = 1L;

        StorySpot storySpot = StorySpot.builder()
            .id(storySpotId)
            .build();

        List<SpotTitleAggregate> mockAggregates = List.of(
            createAggregate(1L, storySpot, "제목1", 100L),
            createAggregate(2L, storySpot, "제목2", 90L),
            createAggregate(3L, storySpot, "제목3", 80L),
            createAggregate(4L, storySpot, "제목4", 70L)
        );

        given(spotTitleAggregateRepository.findTop4TitleBySpotId(eq(storySpotId), any(Pageable.class)))
            .willReturn(mockAggregates);

        // when
        SpotTitleListResponse response = storyService.getSpotTitleList(storySpotId);

        // then
        assertThat(response.titles()).hasSize(4);
        assertThat(response.titles()).containsExactly("제목1", "제목2", "제목3", "제목4");
    }

    private SpotTitleAggregate createAggregate(Long id, StorySpot spot, String titleText, Long count) {
        StoryTitle title = StoryTitle.builder()
            .id(id)
            .title(titleText)
            .build();

        return SpotTitleAggregate.builder()
            .id(id)
            .storySpot(spot)
            .storyTitle(title)
            .storyCount(count)
            .build();
    }
}
