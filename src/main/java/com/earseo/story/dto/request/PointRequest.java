package com.earseo.story.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Schema(description = "위경도 정보")
public record PointRequest(
    @Schema(description = "위도", example = "37.5665", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @DecimalMin(value = "33.0")
    @DecimalMax(value = "39")
    Double latitude,
    @Schema(description = "경도", example = "126.9780", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @DecimalMin(value = "124")
    @DecimalMax(value = "133")
    Double longitude
) {
}
