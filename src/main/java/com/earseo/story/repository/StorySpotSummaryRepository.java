package com.earseo.story.repository;

import com.earseo.story.entity.Locale;
import com.earseo.story.entity.StorySpotSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StorySpotSummaryRepository extends JpaRepository<StorySpotSummary, Long> {
    @Query("""
        SELECT s
        FROM StorySpotSummary s
        WHERE
        s.storySpot.id = :storySpotId
        AND s.locale = :locale
        ORDER BY s.storyConcept
        """)
    List<StorySpotSummary> findByStorySpotIdAndLocale(
        @Param("storySpotId") Long storySpotId,
        @Param("locale") Locale locale
    );
}
