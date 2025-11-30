package com.earseo.story.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record GetRouteListSpotResponse(
    @Schema(description = "요약이 존재하는 이야기 스팟의 정보 (요약이 없는 경로는 요청한 경로 인덱스와 동일한 리스트 인덱스 위치에 null로 반환, 요청 리스트와 순서 동일하게 반환)")
    List<List<GetRouteSpotResponse>> spotList
) {
    static public GetRouteListSpotResponse toDto(List<List<GetRouteSpotResponse>> spotList) {
        return new GetRouteListSpotResponse(spotList);
    }
}
