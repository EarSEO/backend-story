package com.earseo.story.dto.response;

import com.earseo.story.entity.SpotTitleAggregate;
import com.earseo.story.entity.Story;
import com.earseo.story.entity.StorySpotSummary;
import com.earseo.story.repository.projectionDto.StorySpotWithDistanceProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Map;

public record SpotTotalInfoResponse(
    @Schema(description = "기본 이야기 스팟 정보")
    SpotInfoResponse briefSpotInfo,
    @Schema(description = "상위 4개 이야기 제목 목록 (이야기 많은 순서)")
    SpotTitleListResponse spotTitleList,
    @Schema(description = "요청한 좌표로부터 거리 (미터)", example = "50.6")
    Double distance,
    @Schema(description = "이야기 목록 (페이징/정렬 반영)")
    List<StoryInfoResponse> stories,
    @Schema(description = "이 스팟의 요약 목록 (요청 언어 기준, 카테고리별)")
    List<StorySummaryResponse> summaries
) {
    public static SpotTotalInfoResponse toDto(
        StorySpotWithDistanceProjection storySpotWithDistanceProjection,
        List<SpotTitleAggregate> topTitles,
        Slice<Story> storyPage,
        Map<Long, List<String>> imageUrlsMap,
        List<StorySpotSummary> summaries
    ) {
        return new SpotTotalInfoResponse(
            SpotInfoResponse.toDto(storySpotWithDistanceProjection),
            SpotTitleListResponse.toDto(topTitles),
            storySpotWithDistanceProjection.getDistance(),
            storyPage.getContent().stream()
                .map(story -> StoryInfoResponse.toDto(story, imageUrlsMap.getOrDefault(story.getId(), List.of())))
                .toList(),
            summaries.stream()
                .map(StorySummaryResponse::toDto)
                .toList()
        );
    }
}
