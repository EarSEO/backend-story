package com.earseo.story.dto.request;

import com.earseo.story.entity.Locale;
import com.earseo.story.entity.StoryConcept;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CreateRequest(
    @NotNull(message = "작성자 id는 필수입니다")
    @Schema(description = "작성자 ID", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    Long authorId,
    @NotNull(message = "작성자 닉네임은 필수입니다")
    @Schema(description = "작성자 닉네임", example = "이어동", requiredMode = Schema.RequiredMode.REQUIRED)
    String authorName,
    @NotNull(message = "작성자 프로필 url은 필수입니다")
    @Schema(description = "작성자 프로필 이미지 URL", requiredMode = Schema.RequiredMode.REQUIRED)
    String authorProfileUrl,
    @NotNull(message = "작성자 프로필 수정일자는 필수입니다")
    @Schema(description = "작성자 프로필 수정일자", requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDateTime authorProfileUpdatedAt,
    @NotNull(message = "위도는 필수입니다")
    @DecimalMin(value = "33.0", message = "위도는 33 이상이어야 합니다")
    @DecimalMax(value = "39", message = "위도는 39 이하여야 합니다")
    @Schema(description = "위도 (latitude)", example = "37.5665", minimum = "33.0", maximum = "39", requiredMode = Schema.RequiredMode.REQUIRED)
    Double latitude,
    @NotNull(message = "경도는 필수입니다")
    @DecimalMin(value = "124", message = "경도는 124 이상이어야 합니다")
    @DecimalMax(value = "133", message = "경도는 133 이하여야 합니다")
    @Schema(description = "경도 (longitude)", example = "126.9780", minimum = "124", maximum = "133", requiredMode = Schema.RequiredMode.REQUIRED)
    Double longitude,
    @NotNull(message = "이야기 스팟 ID는 필수입니다")
    @Positive(message = "이야기 스팟 ID는 양수여야 합니다")
    @Schema(description = "이야기 스팟 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long storySpotId,
    @NotBlank(message = "제목은 필수입니다")
    @Size(min = 1, max = 30, message = "제목은 1자 이상 30자 이하여야 합니다")
    @Schema(description = "이야기 제목", example = "골목길 맛집", minLength = 1, maxLength = 30, requiredMode = Schema.RequiredMode.REQUIRED)
    String title,
    @NotBlank(message = "내용은 필수입니다")
    @Size(min = 1, max = 200, message = "내용은 1자 이상 200자 이하여야 합니다")
    @Schema(description = "이야기 내용", example = "경복궁 근처 맛있는 카페! 강추..!", minLength = 1, maxLength = 200, requiredMode = Schema.RequiredMode.REQUIRED)
    String content,
    @NotNull(message = "이야기 컨셉은 필수입니다")
    @Schema(description = "이야기 컨셉", example = "TIP", requiredMode = Schema.RequiredMode.REQUIRED)
    StoryConcept storyConcept,
    @NotNull(message = "locale은 필수입니다")
    @Schema(description = "언어정보", example = "KO", requiredMode = Schema.RequiredMode.REQUIRED)
    Locale locale
) {
}
