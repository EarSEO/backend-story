package com.earseo.story.dto.response;

import com.earseo.story.entity.SpotTitleAggregate;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record LocationSpotBriefInfoResponse(
    @Schema(description = "좌표의 이야기 스팟 id (null 이면 없는것)")
    Long spotId,
    @Schema(description = "이야기 제목 목록 (최대 4개, 이야기 개수 및 최근 수정일 기준 정렬)")
    List<String> titles
) {
    public static LocationSpotBriefInfoResponse toDto(Long spotId, List<SpotTitleAggregate> entityList) {
        return new LocationSpotBriefInfoResponse(spotId,
            entityList.stream().map(spotTitleAggregate ->
                spotTitleAggregate.getStoryTitle().getTitle()
            ).toList()
        );
    }
}
