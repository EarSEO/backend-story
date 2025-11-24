package com.earseo.story.repository;

import com.earseo.story.entity.StorySpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StorySpotRepository extends JpaRepository<StorySpot, Long> {
    Optional<StorySpot> findByGeohash(String geohash);

    @Query(value = """
        SELECT s.story_spot_id, s.center, s.geohash, s.created_at
        FROM story_spot s
        WHERE ST_Intersects(
            s.center,
            ST_MakeEnvelope(:minLatitude, :minLongitude, :maxLatitude, :maxLongitude, 4326)
        )
        """, nativeQuery = true)
    List<StorySpot> findByBoundingBox(
        @Param("minLongitude") Double minLongitude,
        @Param("minLatitude") Double minLatitude,
        @Param("maxLongitude") Double maxLongitude,
        @Param("maxLatitude") Double maxLatitude
    );
}
