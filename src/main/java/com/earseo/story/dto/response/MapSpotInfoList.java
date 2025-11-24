package com.earseo.story.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MapSpotInfoList(
    @Schema(description = "이야기 스팟 목록")
    List<SpotInfoResponse> storySpots
) {
}
