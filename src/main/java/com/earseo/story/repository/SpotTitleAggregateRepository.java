package com.earseo.story.repository;

import com.earseo.story.entity.SpotTitleAggregate;
import com.earseo.story.repository.projectionDto.SearchSpotProjection;
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
    List<SpotTitleAggregate> findTopTitleBySpotId(@Param("storySpotId") Long storySpotId, Pageable pageable);

    @Query(value = """
        WITH ranked_titles AS (
            SELECT 
                sta.story_spot_id,
                sta.story_count,
                st.title,
                ss.center,
                ROW_NUMBER() OVER (
                    PARTITION BY sta.story_spot_id 
                    ORDER BY sta.story_count DESC
                ) as rank
            FROM spot_title_aggregate sta
            JOIN story_spot ss ON sta.story_spot_id = ss.story_spot_id
            JOIN story_title st ON sta.story_title_id = st.story_title_id
            WHERE ST_Intersects(
                ss.center,
                ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
            )
        ),
        filtered_titles AS (
            SELECT DISTINCT ON (story_spot_id)
                story_spot_id,
                story_count,
                title,
                center
            FROM ranked_titles
            WHERE rank <= 4
              AND title ILIKE '%' || :keyword || '%'
            ORDER BY story_spot_id, story_count DESC
        )
        SELECT 
            story_spot_id,
            ST_X(center) as longitude,
            ST_Y(center) as latitude,
            ST_Distance(
                center::geography,
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
            ) as distance,
            title
        FROM filtered_titles
        ORDER BY distance
        LIMIT :limit
        """, nativeQuery = true)
    List<SearchSpotProjection> searchByTitleKeywordRectangle(
        @Param("keyword") String keyword,
        @Param("longitude") Double longitude,
        @Param("latitude") Double latitude,
        @Param("minLon") Double minLongitude,
        @Param("minLat") Double minLatitude,
        @Param("maxLon") Double maxLongitude,
        @Param("maxLat") Double maxLatitude,
        @Param("limit") Integer limit
    );
}
