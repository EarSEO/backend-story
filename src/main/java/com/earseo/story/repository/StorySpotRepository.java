package com.earseo.story.repository;

import com.earseo.story.entity.StorySpot;
import com.earseo.story.repository.projectionDto.StorySpotWithDistanceProjection;
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
            ST_MakeEnvelope(:minLongitude, :minLatitude, :maxLongitude, :maxLatitude, 4326)
        )
        """, nativeQuery = true)
    List<StorySpot> findByBoundingBox(
        @Param("minLongitude") Double minLongitude,
        @Param("minLatitude") Double minLatitude,
        @Param("maxLongitude") Double maxLongitude,
        @Param("maxLatitude") Double maxLatitude
    );

    @Query(value = """
        SELECT s.story_spot_id, s.center, s.geohash, s.created_at
        FROM story_spot s
        WHERE ST_DWithin(
            s.center::geography,
            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
            :meters
        )
        """, nativeQuery = true)
    List<StorySpot> findByRadius(
        @Param("longitude") Double longitude,
        @Param("latitude") Double latitude,
        @Param("meters") Double meters
    );

    @Query(value = """
        SELECT s.story_spot_id,
        ST_X(s.center) longitude,
        ST_Y(s.center) latitude,
        s.geohash, s.created_at,
        ST_Distance(
            center::geography,
            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
        ) as distance
        FROM story_spot s
        WHERE s.story_spot_id = :story_spot_id
        """, nativeQuery = true)
    Optional<StorySpotWithDistanceProjection> findByIdWithDistance(
        @Param("story_spot_id") Long storySpotId,
        @Param("longitude") Double longitude,
        @Param("latitude") Double latitude
    );
}
