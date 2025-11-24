package com.earseo.story.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SpotInfoResponse(
    @Schema(description = "경도", example = "126.9780")
    Double longitude,
    @Schema(description = "위도", example = "37.5665")
    Double latitude,
    @Schema(description = "이야기 스팟 ID", example = "1")
    Long storySpotId
) {
}
