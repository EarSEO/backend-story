package com.earseo.story.dto.response;

import com.earseo.story.entity.StoryAuthor;
import io.swagger.v3.oas.annotations.media.Schema;

public record StoryAuthorResponse(
    @Schema(description = "이야기 작성자 ID", example = "10")
    Long storyAuthorId,
    @Schema(description = "작성자 닉네임", example = "이어동")
    String nickname,
    @Schema(description = "작성자 프로필 이미지 주소", example = "https://cdn.example.com/user/profile/10.jpg")
    String profileUrl
) {
    public static StoryAuthorResponse toDto(StoryAuthor storyAuthor) {
        if (storyAuthor == null) return new StoryAuthorResponse(null, null, null);
        return new StoryAuthorResponse(
            storyAuthor.getId(),
            storyAuthor.getNickname(),
            storyAuthor.getProfileUrl()
        );
    }
}
