package com.earseo.story.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "경로를 구성하는 점들의 배열")
public record PathLineStringRequest(
    @Schema(
        description = "경로를 구성하는 좌표 목록 (최소 2개 이상)",
        minLength = 2,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<PointRequest> lineString,
    @Schema(
        description = "이 경로에서 반환할 최대 스팟 개수",
        example = "1",
        defaultValue = "1",
        minimum = "0",
        maximum = "5",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    Long amount
) {
}
