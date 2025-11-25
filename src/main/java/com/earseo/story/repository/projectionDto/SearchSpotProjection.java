package com.earseo.story.repository.projectionDto;

public record SearchSpotProjection(
    Long storySpotId,
    Double longitude,
    Double latitude,
    Double distance,
    String title
) {
}
