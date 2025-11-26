package com.earseo.story.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record UpdateStoryResponse(
        @Schema(description = "이야기 ID", example = "1")
        Long storyId,

        @Schema(description = "수정 일시")
        LocalDateTime updatedAt
) {}