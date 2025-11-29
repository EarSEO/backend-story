package com.earseo.story.dto.response;

import com.earseo.story.entity.Story;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Map;

public record SpotStoryPageResponse(
    @Schema(description = "이야기 목록 (페이징/정렬 반영)")
    List<StoryInfoResponse> stories,
    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    int number,
    @Schema(description = "페이지 크기", example = "10")
    int size,
    @Schema(description = "첫 페이지 여부", example = "true")
    boolean isFirst,
    @Schema(description = "마지막 페이지 여부", example = "false")
    boolean isLast,
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    boolean hasNext,
    @Schema(description = "이전 페이지 존재 여부", example = "false")
    boolean hasPrevious
) {
    public static SpotStoryPageResponse toDto(Slice<Story> storyPage, Map<Long, List<String>> imageUrlsMap) {
        return new SpotStoryPageResponse(
            storyPage.getContent().stream()
                .map(story -> StoryInfoResponse.toDto(story, imageUrlsMap.getOrDefault(story.getId(), List.of())))
                .toList(),
            storyPage.getNumber(),
            storyPage.getSize(),
            storyPage.isFirst(),
            storyPage.isLast(),
            storyPage.hasNext(),
            storyPage.hasPrevious()
        );
    }
}
