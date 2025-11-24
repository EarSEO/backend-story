package com.earseo.story.repository;

import com.earseo.story.entity.SpotTitleAggregate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpotTitleAggregateRepository extends JpaRepository<SpotTitleAggregate, Long> {
    @Modifying
    @Query(value = """
        INSERT INTO spot_title_aggregate (story_spot_id, story_title_id, story_count, updated_at)
        VALUES (:story_spot_id, :story_title_id, 1, CURRENT_TIMESTAMP)
        ON CONFLICT ON CONSTRAINT spot_title_unique_constraint
        DO UPDATE SET
            story_count = spot_title_aggregate.story_count + 1,
            updated_at = CURRENT_TIMESTAMP
        """, nativeQuery = true)
    void incrementOrCreate(@Param("story_spot_id") Long storySpotId, @Param("story_title_id") Long storyTitleId);

    @Query("""
            SELECT s
            FROM SpotTitleAggregate s
            WHERE s.storySpot.id = :storySpotId
            ORDER BY s.storyCount DESC, s.updatedAt DESC
        """)
    @EntityGraph(attributePaths = {"storyTitle"})
    List<SpotTitleAggregate> findTop4TitleBySpotId(@Param("storySpotId") Long storySpotId, Pageable pageable);
}
