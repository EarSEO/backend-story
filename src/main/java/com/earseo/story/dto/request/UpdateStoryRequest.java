package com.earseo.story.dto.request;

import com.earseo.story.entity.StoryConcept;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateStoryRequest(
        @Size(min = 1, max = 30, message = "제목은 1자 이상 30자 이하여야 합니다")
        @Schema(description = "이야기 제목", example = "수정된 제목")
        String title,

        @Size(min = 1, max = 200, message = "내용은 1자 이상 200자 이하여야 합니다")
        @Schema(description = "이야기 내용", example = "수정된 내용입니다")
        String content,

        @Schema(description = "이야기 컨셉", example = "EXPERIENCE")
        StoryConcept storyConcept,

        @Schema(description = "유지할 이미지 URL 목록")
        List<String> keepImageUrls
) {}