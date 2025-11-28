package com.earseo.story.repository;

import com.earseo.story.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("이야기 요약을 할 때 ")
class StorySpotSummaryJdbcRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private StoryJdbcRepository storyJdbcRepository;

    @Autowired
    private StorySpotSummaryJdbcRepository storySpotSummaryJdbcRepository;

    private StorySpot testSpot1;
    private StorySpot testSpot2;
    private StoryTitle testTitle;
    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        testSpot1 = StorySpot.builder()
            .geohash("wydm6m")
            .center(createPoint(126.9780, 37.5665))
            .build();
        entityManager.persist(testSpot1);

        testSpot2 = StorySpot.builder()
            .geohash("wydm7n")
            .center(createPoint(126.9784, 37.5635))
            .build();
        entityManager.persist(testSpot2);

        testTitle = StoryTitle.builder()
            .title("Test Title")
            .build();
        entityManager.persist(testTitle);

        entityManager.flush();
        entityManager.clear();
    }

    private Point createPoint(double longitude, double latitude) {
        return geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }

    @Test
    @DisplayName("기준 이상의 이야기를 가진 핫스팟을 조회한다")
    void findHotStorySpot_AboveThreshold_ReturnsHotSpots() {
        // given
        int hotSpotThreshold = 30;
        createStories(testSpot1, hotSpotThreshold + 5);
        createStories(testSpot2, hotSpotThreshold - 5);
        entityManager.flush();
        entityManager.clear();

        // when
        List<Long> hotSpotIds = storyJdbcRepository.findHotStorySpot(hotSpotThreshold);

        // then
        assertThat(hotSpotIds).hasSize(1);
        assertThat(hotSpotIds).contains(testSpot1.getId());
        assertThat(hotSpotIds).doesNotContain(testSpot2.getId());
    }

    @Test
    @DisplayName("핫스팟이 없으면 빈 리스트를 반환한다")
    void findHotStorySpot_NoHotSpots_ReturnsEmptyList() {
        // given
        int hotSpotThreshold = 30;
        createStories(testSpot1, hotSpotThreshold - 10);
        createStories(testSpot2, hotSpotThreshold - 5);
        entityManager.flush();
        entityManager.clear();

        // when
        List<Long> hotSpotIds = storyJdbcRepository.findHotStorySpot(hotSpotThreshold);

        // then
        assertThat(hotSpotIds).isEmpty();
    }

    @Test
    @DisplayName("각 컨셉별로 최신 N개의 스토리를 조회한다")
    void findAllHotStorySummarizeTargetStory_ReturnsTopNStoriesPerConcept() {
        // given
        int storySummaryLimit = 3; // 요약 대상 이야기 개수
        Story story1 = createAndPersistStory(testSpot1, StoryConcept.HISTORY, "HISTORY1", LocalDateTime.now().minusDays(3));
        Story story2 = createAndPersistStory(testSpot1, StoryConcept.HISTORY, "HISTORY2", LocalDateTime.now().minusDays(2));
        Story story3 = createAndPersistStory(testSpot1, StoryConcept.HISTORY, "HISTORY3", LocalDateTime.now().minusDays(1));
        Story story4 = createAndPersistStory(testSpot1, StoryConcept.HISTORY, "HISTORY4", LocalDateTime.now()); // 가장 최신

        Story story5 = createAndPersistStory(testSpot1, StoryConcept.EXPERIENCE, "EXPERIENCE", LocalDateTime.now());

        entityManager.flush();
        entityManager.clear();

        // when
        Map<Long, Map<StoryConcept, Set<Story>>> result =
            storyJdbcRepository.findAllHotStorySummarizeTargetStory(
                List.of(testSpot1.getId()),
                storySummaryLimit
            );

        // then
        assertThat(result).containsKey(testSpot1.getId());

        Set<Story> attractionStories = result.get(testSpot1.getId()).get(StoryConcept.HISTORY);
        assertThat(attractionStories).hasSize(storySummaryLimit);
        assertThat(attractionStories).extracting(Story::getId)
            .containsExactlyInAnyOrder(story2.getId(), story3.getId(), story4.getId());
        assertThat(attractionStories).extracting(Story::getId)
            .doesNotContain(story1.getId());  // 가장 오래된 것은 제외

        // FOOD 카테고리: 1개만 있음
        Set<Story> foodStories = result.get(testSpot1.getId()).get(StoryConcept.EXPERIENCE);
        assertThat(foodStories).hasSize(1);
        assertThat(foodStories).extracting(Story::getId)
            .containsExactly(story5.getId());
    }

    @Test
    @DisplayName("여러 핫스팟의 스토리를 한 번에 조회한다")
    void findAllHotStorySummarizeTargetStory_MultipleSpots_ReturnsAllStories() {
        // given
        createAndPersistStory(testSpot1, StoryConcept.HISTORY, "spot1-1", LocalDateTime.now());
        createAndPersistStory(testSpot1, StoryConcept.HISTORY, "spot1-2", LocalDateTime.now());
        createAndPersistStory(testSpot1, StoryConcept.EXPERIENCE, "spot1-3", LocalDateTime.now());

        createAndPersistStory(testSpot2, StoryConcept.HISTORY, "spot2-1", LocalDateTime.now());
        createAndPersistStory(testSpot2, StoryConcept.EXPERIENCE, "spot2-2", LocalDateTime.now());

        entityManager.flush();
        entityManager.clear();

        // when
        Map<Long, Map<StoryConcept, Set<Story>>> result =
            storyJdbcRepository.findAllHotStorySummarizeTargetStory(
                List.of(testSpot1.getId(), testSpot2.getId()),
                3
            );

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsKeys(testSpot1.getId(), testSpot2.getId());
        assertThat(result.get(testSpot1.getId()).get(StoryConcept.HISTORY)).hasSize(2);
        assertThat(result.get(testSpot1.getId()).get(StoryConcept.EXPERIENCE)).hasSize(1);
        assertThat(result.get(testSpot2.getId()).get(StoryConcept.HISTORY)).hasSize(1);
        assertThat(result.get(testSpot2.getId()).get(StoryConcept.EXPERIENCE)).hasSize(1);
    }

    @Test
    @DisplayName("이전 요약의 이야기 id 를 조회한다")
    void findAllLastSummarizedStoryIdSet_ReturnsLastSummaries() {
        // given
        StorySpotSummary summary = StorySpotSummary.builder()
            .storySpot(testSpot1)
            .storyConcept(StoryConcept.HISTORY)
            .locale(Locale.KO)
            .title("Test Title")
            .summary("Test Summary")
            .summarizedStoryIdSet(Set.of(1L, 2L, 3L))
            .build();
        entityManager.persist(summary);
        entityManager.flush();
        entityManager.clear();

        // when
        Map<Long, Map<StoryConcept, Set<Long>>> result =
            storySpotSummaryJdbcRepository.findAllLastSummarizedStoryIdSet(
                List.of(testSpot1.getId())
            );

        // then
        assertThat(result).containsKey(testSpot1.getId());
        assertThat(result.get(testSpot1.getId())).containsKey(StoryConcept.HISTORY);
        assertThat(result.get(testSpot1.getId()).get(StoryConcept.HISTORY))
            .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("새로운 요약을 저장한다")
    void batchUpsert_NewData_Inserts() {
        // given
        Set<Story> stories = Set.of(
            createAndPersistStory(testSpot1, StoryConcept.HISTORY, "content1", LocalDateTime.now()),
            createAndPersistStory(testSpot1, StoryConcept.HISTORY, "content2", LocalDateTime.now()),
            createAndPersistStory(testSpot1, StoryConcept.HISTORY, "content3", LocalDateTime.now())
        );

        StorySpotSummaryJdbcRepository.SpotSummarySaveRequest request =
            new StorySpotSummaryJdbcRepository.SpotSummarySaveRequest(
                testSpot1.getId(),
                StoryConcept.HISTORY,
                Locale.KO,
                "Test Title",
                "Test Summary",
                stories
            );

        // when
        long insertCount = storySpotSummaryJdbcRepository.batchUpsert(List.of(request));
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(insertCount).isEqualTo(1);

        List<StorySpotSummary> summaries = entityManager
            .createQuery("SELECT s FROM StorySpotSummary s WHERE s.storySpot.id = :spotId", StorySpotSummary.class)
            .setParameter("spotId", testSpot1.getId())
            .getResultList();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getTitle()).isEqualTo("Test Title");
        assertThat(summaries.get(0).getSummary()).isEqualTo("Test Summary");
    }

    @Test
    @DisplayName("새로운 요약으로 덮어쓴다")
    void batchUpsert_ExistingData_Updates() {
        // given
        // 기존 요약 저장
        Set<Story> oldStories = Set.of(
            createAndPersistStory(testSpot1, StoryConcept.HISTORY, "old1", LocalDateTime.now())
        );
        entityManager.flush();
        entityManager.clear();

        StorySpotSummaryJdbcRepository.SpotSummarySaveRequest oldRequest =
            new StorySpotSummaryJdbcRepository.SpotSummarySaveRequest(
                testSpot1.getId(),
                StoryConcept.HISTORY,
                Locale.KO,
                "Old Title",
                "Old Summary",
                oldStories
            );

        storySpotSummaryJdbcRepository.batchUpsert(List.of(oldRequest));

        // when
        // 같은 키로 다시 저장
        Set<Story> newStories = Set.of(
            createAndPersistStory(testSpot1, StoryConcept.HISTORY, "new1", LocalDateTime.now()),
            createAndPersistStory(testSpot1, StoryConcept.HISTORY, "new2", LocalDateTime.now())
        );
        entityManager.flush();
        entityManager.clear();

        StorySpotSummaryJdbcRepository.SpotSummarySaveRequest newRequest =
            new StorySpotSummaryJdbcRepository.SpotSummarySaveRequest(
                testSpot1.getId(),
                StoryConcept.HISTORY,
                Locale.KO,
                "Updated Title",
                "Updated Summary",
                newStories
            );

        long updateCount = storySpotSummaryJdbcRepository.batchUpsert(List.of(newRequest));

        // then
        // 1개만 존재 (UPDATE)
        assertThat(updateCount).isEqualTo(1);

        List<StorySpotSummary> summaries = entityManager
            .createQuery("SELECT s FROM StorySpotSummary s WHERE s.storySpot.id = :spotId AND s.storyConcept = :concept AND s.locale = :locale", StorySpotSummary.class)
            .setParameter("spotId", testSpot1.getId())
            .setParameter("concept", StoryConcept.HISTORY)
            .setParameter("locale", Locale.KO)
            .getResultList();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getTitle()).isEqualTo("Updated Title");
        assertThat(summaries.get(0).getSummary()).isEqualTo("Updated Summary");
    }

    private void createStories(StorySpot spot, int count) {
        for (int i = 0; i < count; i++) {
            createAndPersistStory(spot, StoryConcept.HISTORY, "content" + i, LocalDateTime.now());
        }
    }

    private Story createAndPersistStory(StorySpot spot, StoryConcept concept, String content, LocalDateTime createdAt) {
        Story story = Story.builder()
            .storySpot(spot)
            .storyConcept(concept)
            .storyTitle(testTitle)
            .locale(Locale.KO)
            .content(content)
            .createdAt(createdAt)
            .build();
        entityManager.persist(story);
        return story;
    }
}
