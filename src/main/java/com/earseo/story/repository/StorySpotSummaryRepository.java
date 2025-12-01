package com.earseo.story.repository;

import com.earseo.story.entity.Locale;
import com.earseo.story.entity.StorySpotSummary;
import com.earseo.story.repository.projectionDto.StorySpotWithSummaryProjection;
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

    @Query(value = """
            WITH nearby_spots AS (
                SELECT 
                    ss.story_spot_id,
                    ss.center,
                    ss.created_at,
                    ST_Distance(
                        ss.center::geography,
                        CAST(:path AS geography)
                    ) AS distance
                FROM story_spot ss
                WHERE ST_DWithin(ss.center::geography, CAST(:path AS geography), :meters)
                ORDER BY distance ASC
            ),
            top_titles AS (
                SELECT DISTINCT ON (sta.story_spot_id)
                    sta.story_spot_id,
                    st.title AS top_title,
                    sta.story_count
                FROM spot_title_aggregate sta
                JOIN story_title st ON sta.story_title_id = st.story_title_id
                WHERE sta.story_spot_id IN (SELECT story_spot_id FROM nearby_spots)
                ORDER BY sta.story_spot_id, sta.story_count DESC, sta.updated_at DESC
            )
            SELECT 
                ns.story_spot_id AS storySpotId,
                ST_X(ns.center) AS longitude,
                ST_Y(ns.center) AS latitude,
                ns.created_at AS createdAt,
                tt.top_title AS topTitle,
                sss.story_spot_summary_id AS summaryId,
                sss.title AS summaryTitle,
                sss.summary AS summary,
                sss.story_concept AS storyConcept,
                sss.docent_url AS docentUrl
            FROM nearby_spots ns
            LEFT JOIN top_titles tt ON ns.story_spot_id = tt.story_spot_id
            LEFT JOIN story_spot_summary sss ON ns.story_spot_id = sss.story_spot_id
                AND sss.locale = :locale
                AND (:storyConcept IS NULL OR sss.story_concept = :storyConcept)
            WHERE sss.story_spot_summary_id IS NOT NULL
            ORDER BY tt.story_count DESC, ns.distance ASC
            LIMIT :amount
            """, nativeQuery = true)
    List<StorySpotWithSummaryProjection> findSpotsNearPathWithSummaries(
            @Param("path") org.locationtech.jts.geom.LineString path,
            @Param("meters") Long meters,
            @Param("locale") String locale,
            @Param("storyConcept") String storyConcept,
            @Param("amount") Long amount
    );


    @Query(value = """
            SELECT * FROM story_spot_summary sss
            WHERE sss.updated_at >= NOW() - make_interval(mins => :minutes)
            """, nativeQuery = true)
    List<StorySpotSummary> findAllByUpdatedAt(
           @Param("minutes") Integer minutes
    );
}
