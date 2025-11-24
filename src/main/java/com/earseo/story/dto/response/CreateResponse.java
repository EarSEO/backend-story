package com.earseo.story.dto.response;

import com.earseo.story.entity.Story;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CreateResponse(
    @Schema(description = "이야기 id", example = "1")
    Long storyId,
    @Schema(description = "이야기 스팟 id", example = "1")
    Long storySpotId,
    @Schema(description = "이야기 생성 시각")
    LocalDateTime createdAt
) {
    public static CreateResponse toDto(Story story) {
        return new CreateResponse(
            story.getId(),
            story.getStorySpot().getId(),
            story.getCreatedAt()
        );
    }
}
