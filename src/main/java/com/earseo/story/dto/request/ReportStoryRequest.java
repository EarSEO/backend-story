package com.earseo.story.dto.request;

import com.earseo.story.entity.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportStoryRequest(
        @NotNull(message = "신고 사유는 필수입니다")
        @Schema(description = "신고 사유", example = "INSULT", requiredMode = Schema.RequiredMode.REQUIRED)
        ReportReason reason,

        @Size(max = 500, message = "상세 내용은 500자 이하여야 합니다")
        @Schema(description = "신고 상세 내용", example = "욕설이 포함되어 있습니다")
        String description
) {}