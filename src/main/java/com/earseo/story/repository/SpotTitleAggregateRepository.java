package com.earseo.story.repository;

import com.earseo.story.entity.SpotTitleAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpotTitleAggregateRepository extends JpaRepository<SpotTitleAggregate, Long> {
    @Modifying
    @Query(value = """
        INSERT INTO spot_title_aggregate (story_spot_id, story_title_id, story_count)
        VALUES (:story_spot_id, :story_title_id, 1)
        ON CONFLICT ON CONSTRAINT spot_title_unique_constraint
        DO UPDATE SET
            story_count = spot_title_aggregate.story_count + 1
        """, nativeQuery = true)
    void incrementOrCreate(@Param("story_spot_id") Long storySpotId, @Param("story_title_id") Long storyTitleId);
}
