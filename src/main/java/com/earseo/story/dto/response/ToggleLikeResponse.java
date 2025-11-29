package com.earseo.story.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ToggleLikeResponse(
        @Schema(description = "좋아요 상태", example = "true")
        boolean isLiked,

        @Schema(description = "현재 좋아요 개수", example = "15")
        Long likeCount
) {}