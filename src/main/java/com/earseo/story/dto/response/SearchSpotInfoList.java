package com.earseo.story.dto.response;

import com.earseo.story.repository.projectionDto.SearchSpotProjection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record SearchSpotInfoList(
    @Schema(description = "이야기 스팟 목록")
    List<SearchSpotInfoResponse> storySpots
) {
    public static SearchSpotInfoList toDto(List<SearchSpotProjection> projections) {
        return new SearchSpotInfoList(projections.stream()
            .map(SearchSpotInfoResponse::toDto)
            .toList());
    }
}
