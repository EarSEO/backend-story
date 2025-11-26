package com.earseo.story.dto.response;

import com.earseo.story.entity.Locale;
import com.earseo.story.entity.Story;
import com.earseo.story.entity.StoryConcept;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record StoryInfoResponse(
    @Schema(description = "이야기 작성자")
    StoryAuthorResponse storyAuthor,
    @Schema(description = "이야기 제목", example = "경복궁의 맛집")
    String title,
    @Schema(description = "이야기 내용", example = "여기 건물 앞에 있는 포장마차가 떡볶이 맛집이에요.")
    String content,
    @Schema(description = "작성 언어", example = "KO")
    Locale locale,
    @Schema(description = "이야기 카테고리", example = "TIP")
    StoryConcept storyConcept,
    @Schema(description = "좋아요 개수", example = "37")
    Long likeCount,
    @Schema(description = "작성 시각", example = "[2025,11,25,10,32,11,385554000]")
    LocalDateTime createdAt,
    @Schema(description = "수정 시각", example = "[2025,11,25,18,32,19,123454000]")
    LocalDateTime updatedAt,
    @Schema(description = "이야기 이미지 주소 목록")
    List<String> imageUrls
) {
    public static StoryInfoResponse toDto(Story story, List<String> imageUrls) {
        StoryAuthorResponse authorResponse = story.getStoryAuthor() != null
            ? StoryAuthorResponse.toDto(story.getStoryAuthor()) : null;

        return new StoryInfoResponse(
            authorResponse,
            story.getStoryTitle().getTitle(),
            story.getContent(),
            story.getLocale(),
            story.getStoryConcept(),
            story.getLikeCount(),
            story.getCreatedAt(),
            story.getUpdatedAt(),
            imageUrls
        );
    }
}
