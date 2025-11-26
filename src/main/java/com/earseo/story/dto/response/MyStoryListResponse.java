package com.earseo.story.dto.response;

import com.earseo.story.entity.Story;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

public record MyStoryListResponse(
        @Schema(description = "이야기 목록")
        List<MyStoryResponse> stories,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,

        @Schema(description = "다음 조회 시 사용할 마지막 이야기 ID (null이면 마지막)", example = "42")
        Long lastStoryId
) {
    public static MyStoryListResponse toDto(List<Story> stories, Map<Long, List<String>> imageUrlMap, boolean hasNext) {
        List<MyStoryResponse> storyResponses = stories.stream()
                .map(story -> MyStoryResponse.toDto(
                        story,
                        imageUrlMap.getOrDefault(story.getId(), List.of())
                ))
                .toList();

        Long lastStoryId = stories.isEmpty() ? null : stories.get(stories.size() - 1).getId();

        return new MyStoryListResponse(
                storyResponses,
                hasNext,
                lastStoryId
        );
    }
}