package com.earseo.story.dto.request;

import com.earseo.story.entity.StoryConcept;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.util.List;

public record GetRouteListSpotRequest(
    @Schema(
        description = "경로 목록",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 1
    )
    @NotNull
    List<PathLineStringRequest> paths,
    @Schema(
        description = "요청할 이야기 컨셉 (null로 보낼시 랜덤하게 선정 후 반환)",
        example = "TIP",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    StoryConcept storyConcept,
    @Schema(
        description = "탐색을 할 요약이 있는 스팟의 경로로 부터 떨어진 최대 거리",
        example = "50",
        defaultValue = "50",
        minimum = "5",
        maximum = "1000",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    Long meters
) {
}
