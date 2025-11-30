package com.earseo.story.dto.response;

import com.earseo.story.entity.StoryConcept;
import com.earseo.story.repository.projectionDto.StorySpotWithSummaryProjection;
import io.swagger.v3.oas.annotations.media.Schema;

public record GetRouteSpotResponse(
    @Schema(description = "경도", example = "126.9780")
    Double longitude,
    @Schema(description = "위도", example = "37.5665")
    Double latitude,
    @Schema(description = "이야기 스팟 ID", example = "1")
    Long storySpotId,
    @Schema(description = "이야기 요약 ID", example = "1")
    Long summaryId,
    @Schema(description = "이야기 스팟의 가장 많은 이야기 주제", example = "맛집")
    String title,
    @Schema(description = "요약 제목", example = "역사적 명소의 종합 요약")
    String summaryTitle,
    @Schema(description = "요약의 카테고리", example = "HISTORY")
    StoryConcept storyConcept,
    @Schema(description = "요약 도슨트 주소", example = "https://cdn.example.com/docent/audio/1234.mp3")
    String docentUrl
) {
    public static GetRouteSpotResponse toDto(StorySpotWithSummaryProjection spot) {
        return new GetRouteSpotResponse(
            spot.getLongitude(),
            spot.getLatitude(),
            spot.getStorySpotId(),
            spot.getSummaryId(),
            spot.getTopTitle(),
            spot.getSummaryTitle(),
            spot.getStoryConcept(),
            spot.getDocentUrl()
        );
    }
}
