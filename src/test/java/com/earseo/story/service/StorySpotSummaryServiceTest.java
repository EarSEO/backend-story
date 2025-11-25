package com.earseo.story.service;

import com.earseo.story.entity.Locale;
import com.earseo.story.entity.Story;
import com.earseo.story.entity.StoryConcept;
import com.earseo.story.repository.StoryJdbcRepository;
import com.earseo.story.repository.StorySpotSummaryJdbcRepository;
import com.earseo.story.service.OpenAiService.SpotSummaryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("이야기 요약 시 ")
class StorySpotSummaryServiceTest {

    @Mock
    private OpenAiService openAiService;

    @Mock
    private StoryJdbcRepository storyJDBCRepository;

    @Mock
    private StorySpotSummaryJdbcRepository storySpotSummaryJDBCRepository;

    @InjectMocks
    private StorySpotSummaryService storySpotSummaryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(storySpotSummaryService, "HOT_SPOT_THRESHOLD", 30);
        ReflectionTestUtils.setField(storySpotSummaryService, "STORY_SUMMARY_LIMIT", 3);
        ReflectionTestUtils.setField(storySpotSummaryService, "STORY_SUMMARY_THRESHOLD", 3);
    }

    @Test
    @DisplayName("핫스팟이 없으면 요약을 생성하지 않는다")
    void createSummariesScheduled_NoHotSpots_DoesNotCreateSummaries() {
        // given
        given(storyJDBCRepository.findHotStorySpot(30))
            .willReturn(Collections.emptyList());

        // when
        storySpotSummaryService.createSummariesScheduled();

        // then
        then(storySpotSummaryJDBCRepository).should(never())
            .findAllLastSummarizedStoryIdSet(anyList());
        then(storyJDBCRepository).should(never())
            .findAllHotStorySummarizeTargetStory(anyList(), anyInt());
        then(storySpotSummaryJDBCRepository).should(never())
            .batchUpsert(anyList());
    }

    @Test
    @DisplayName("이야기 스팟의 이야기 컨셉의 이야기 개수가 조건을 만족하지 않으면 요약을 생성하지 않는다")
    void createSummariesScheduled_BelowThreshold_DoesNotCreateSummary() {
        // given
        List<Long> hotSpotIds = List.of(1L);

        Story story1 = createStory(1L, "content1");
        Story story2 = createStory(2L, "content2");
        Set<Story> stories = Set.of(story1, story2);

        Map<Long, Map<StoryConcept, Set<Story>>> currentStories = Map.of(
            1L, Map.of(StoryConcept.EXPERIENCE, stories)
        );

        given(storyJDBCRepository.findHotStorySpot(30)).willReturn(hotSpotIds);
        given(storySpotSummaryJDBCRepository.findAllLastSummarizedStoryIdSet(hotSpotIds))
            .willReturn(Collections.emptyMap());
        given(storyJDBCRepository.findAllHotStorySummarizeTargetStory(hotSpotIds, 3))
            .willReturn(currentStories);

        // when
        storySpotSummaryService.createSummariesScheduled();

        // then
        then(openAiService).should(never())
            .generateSummaryWithAI(anySet(), any(), any());
        then(storySpotSummaryJDBCRepository).should(never())
            .batchUpsert(anyList());
    }

    @Test
    @DisplayName("이전의 요약 생성 조건과 같은 이야기 구성이면 요약을 생성하지 않는다")
    void createSummariesScheduled_SameStoryIds_DoesNotCreateSummary() {
        // given
        List<Long> hotSpotIds = List.of(1L);

        Map<Long, Map<StoryConcept, Set<Long>>> lastSummaries = Map.of(
            1L, Map.of(StoryConcept.EXPERIENCE, Set.of(1L, 2L, 3L))
        );

        // 이전 요약과 동일한 이야기
        Story story1 = createStory(1L, "content1");
        Story story2 = createStory(2L, "content2");
        Story story3 = createStory(3L, "content3");
        Set<Story> stories = Set.of(story1, story2, story3);

        Map<Long, Map<StoryConcept, Set<Story>>> currentStories = Map.of(
            1L, Map.of(StoryConcept.EXPERIENCE, stories)
        );

        given(storyJDBCRepository.findHotStorySpot(30)).willReturn(hotSpotIds);
        given(storySpotSummaryJDBCRepository.findAllLastSummarizedStoryIdSet(hotSpotIds))
            .willReturn(lastSummaries);
        given(storyJDBCRepository.findAllHotStorySummarizeTargetStory(hotSpotIds, 3))
            .willReturn(currentStories);

        // when
        storySpotSummaryService.createSummariesScheduled();

        // then
        then(openAiService).should(never())
            .generateSummaryWithAI(anySet(), any(), any());
        then(storySpotSummaryJDBCRepository).should(never())
            .batchUpsert(anyList());
    }

    @Test
    @DisplayName("새로운 스토리가 있으면 모든 Locale에 대해 요약을 생성한다")
    void createSummariesScheduled_NewStories_CreatesSummariesForAllLocales() {
        // given
        List<Long> hotSpotIds = List.of(1L);

        Story story1 = createStory(1L, "content1");
        Story story2 = createStory(2L, "content2");
        Story story3 = createStory(3L, "content3");
        Set<Story> stories = Set.of(story1, story2, story3);

        Map<Long, Map<StoryConcept, Set<Story>>> currentStories = Map.of(
            1L, Map.of(StoryConcept.EXPERIENCE, stories)
        );

        // 이전 요약과 다른 ID
        Map<Long, Map<StoryConcept, Set<Long>>> lastSummaries = Map.of(
            1L, Map.of(StoryConcept.EXPERIENCE, Set.of(4L, 5L, 6L))
        );

        SpotSummaryResult mockResult = new SpotSummaryResult("Test Title", "Test Summary");

        given(storyJDBCRepository.findHotStorySpot(30)).willReturn(hotSpotIds);
        given(storySpotSummaryJDBCRepository.findAllLastSummarizedStoryIdSet(hotSpotIds))
            .willReturn(lastSummaries);
        given(storyJDBCRepository.findAllHotStorySummarizeTargetStory(hotSpotIds, 3))
            .willReturn(currentStories);
        given(openAiService.generateSummaryWithAI(anySet(), any(), any()))
            .willReturn(mockResult);
        given(storySpotSummaryJDBCRepository.batchUpsert(anyList()))
            .willReturn(2L);

        // when
        storySpotSummaryService.createSummariesScheduled();

        // then
        // Locale 개수만큼 호출
        then(openAiService).should(times(2))
            .generateSummaryWithAI(eq(stories), eq(StoryConcept.EXPERIENCE), any(Locale.class));

        then(storySpotSummaryJDBCRepository).should(times(1))
            .batchUpsert(argThat(list -> list.size() == 2));
    }

    private Story createStory(Long id, String content) {
        return Story.builder()
            .id(id)
            .content(content)
            .storyConcept(StoryConcept.EXPERIENCE)
            .locale(Locale.KO)
            .createdAt(LocalDateTime.now())
            .build();
    }
}
