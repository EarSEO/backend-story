package com.earseo.story.dto.response;

import com.earseo.story.entity.Story;
import com.earseo.story.entity.StoryConcept;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record MyStoryResponse(
        @Schema(description = "이야기 ID", example = "1")
        Long storyId,

        @Schema(description = "작성자 닉네임", example = "카피바라")
        String nickname,

        @Schema(description = "이야기 제목", example = "경복궁 근처 맛집")
        String title,

        @Schema(description = "이야기 내용", example = "정말 맛있는 카페입니다!")
        String content,

        @Schema(description = "이야기 컨셉", example = "TIP")
        StoryConcept storyConcept,

        @Schema(description = "이미지 URL 목록")
        List<String> imageUrls,

        @Schema(description = "좋아요 개수", example = "15")
        Long likeCount,

        @Schema(description = "이야기 스팟 ID", example = "1")
        Long storySpotId,

        @Schema(description = "위도", example = "37.5665")
        Double latitude,

        @Schema(description = "경도", example = "126.9780")
        Double longitude,

        @Schema(description = "생성 일시")
        LocalDateTime createdAt,

        @Schema(description = "수정 일시")
        LocalDateTime updatedAt
) {
    public static MyStoryResponse toDto(Story story, List<String> imageUrls) {
        return new MyStoryResponse(
                story.getId(),
                story.getStoryAuthor().getNickname(),
                story.getStoryTitle().getTitle(),
                story.getContent(),
                story.getStoryConcept(),
                imageUrls,
                story.getLikeCount(),
                story.getStorySpot().getId(),
                story.getPoint().getY(),
                story.getPoint().getX(),
                story.getCreatedAt(),
                story.getUpdatedAt()
        );
    }
}