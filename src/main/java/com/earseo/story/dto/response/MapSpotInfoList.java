package com.earseo.story.dto.response;

import com.earseo.story.entity.StorySpot;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.stream.Collectors;

public record MapSpotInfoList(
    @Schema(description = "이야기 스팟 목록")
    List<SpotInfoResponse> storySpots
) {
    public static MapSpotInfoList toDto(List<StorySpot> entities) {
        return new MapSpotInfoList(entities.stream().map(SpotInfoResponse::toDto).toList());
    }
}
