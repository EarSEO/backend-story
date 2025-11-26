package com.earseo.story.dto.response;

import com.earseo.story.entity.StorySpot;
import com.earseo.story.repository.projectionDto.StorySpotWithDistanceProjection;
import io.swagger.v3.oas.annotations.media.Schema;

public record SpotInfoResponse(
    @Schema(description = "경도", example = "126.9780")
    Double longitude,
    @Schema(description = "위도", example = "37.5665")
    Double latitude,
    @Schema(description = "이야기 스팟 ID", example = "1")
    Long storySpotId
) {
    public static SpotInfoResponse toDto(StorySpot entity) {
        return new SpotInfoResponse(entity.getCenter().getX(), entity.getCenter().getY(), entity.getId());
    }

    public static SpotInfoResponse toDto(StorySpotWithDistanceProjection storySpotWithDistanceProjection) {
        return new SpotInfoResponse(storySpotWithDistanceProjection.getLongitude(), storySpotWithDistanceProjection.getLatitude(), storySpotWithDistanceProjection.getStorySpotId());
    }
}
