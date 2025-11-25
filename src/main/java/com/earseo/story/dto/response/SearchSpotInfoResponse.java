package com.earseo.story.dto.response;

import com.earseo.story.repository.projectionDto.SearchSpotProjection;
import io.swagger.v3.oas.annotations.media.Schema;

public record SearchSpotInfoResponse(
    @Schema(description = "경도", example = "126.9780")
    Double longitude,
    @Schema(description = "위도", example = "37.5665")
    Double latitude,
    @Schema(description = "이야기 스팟 ID", example = "1")
    Long storySpotId,
    @Schema(description = "요청한 좌표로부터 거리 (미터)", example = "50.6")
    Double distance,
    @Schema(description = "이야기 스팟이 검색 키워드와 유사도 검색으로 검색 대상이 된 이야기 주제 중 이야기가 가장 많은 이야기 주제", example = "맛집")
    String title
) {
    public static SearchSpotInfoResponse toDto(SearchSpotProjection projection) {
        return new SearchSpotInfoResponse(
            projection.longitude(),
            projection.latitude(),
            projection.storySpotId(),
            projection.distance(),
            projection.title()
        );
    }
}
