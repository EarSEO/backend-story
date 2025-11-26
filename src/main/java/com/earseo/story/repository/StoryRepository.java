package com.earseo.story.repository;

import com.earseo.story.entity.Story;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoryRepository extends JpaRepository<Story, Long> {
    @EntityGraph(attributePaths = {"storyAuthor", "storyTitle", "storySpot"})
    @Query("""
        SELECT s FROM Story s
        WHERE s.storySpot.id = :storySpotId
        """)
    Page<Story> findByStorySpotIdAndLocale(
        @Param("storySpotId") Long storySpotId,
        Pageable pageable
    );
}
