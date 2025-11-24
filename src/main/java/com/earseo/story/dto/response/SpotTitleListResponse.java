package com.earseo.story.dto.response;

import com.earseo.story.entity.SpotTitleAggregate;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record SpotTitleListResponse(
    @Schema(description = "이야기 제목 목록 (최대 4개, 이야기 개수 및 최근 수정일 기준 정렬)")
    List<String> titles
) {
    public static SpotTitleListResponse toDto(List<SpotTitleAggregate> entityList) {
        return new SpotTitleListResponse(
            entityList.stream().map(spotTitleAggregate ->
                spotTitleAggregate.getStoryTitle().getTitle()
            ).toList()
        );
    }
}
