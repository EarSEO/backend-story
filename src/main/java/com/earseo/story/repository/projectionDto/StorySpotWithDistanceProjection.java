package com.earseo.story.repository.projectionDto;

import java.time.LocalDateTime;

public interface StorySpotWithDistanceProjection {
    Long getStorySpotId();
    Double getLongitude();
    Double getLatitude();
    String getGeohash();
    LocalDateTime getCreatedAt();
    Double getDistance();
}
