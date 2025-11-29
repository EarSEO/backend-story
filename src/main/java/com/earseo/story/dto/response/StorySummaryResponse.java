package com.earseo.story.dto.response;

import com.earseo.story.entity.StoryConcept;
import com.earseo.story.entity.StorySpotSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;

public record StorySummaryResponse(
    @Schema(description = "이야기 스팟 요약 ID", example = "1234")
    Long storySpotSummaryId,
    @Schema(description = "요약의 카테고리", example = "HISTORY")
    StoryConcept storyConcept,
    @Schema(description = "도슨트 주소", example = "https://cdn.example.com/docent/audio/1234.mp3")
    String docentUrl,
    @Schema(description = "요약 제목", example = "역사적 명소의 종합 요약")
    String title,
    @Schema(description = "스토리 요약 본문", example = "이곳은 과거 문화유산으로 유명합니다.")
    String summary,
    @Schema(description = "요약 대상 스토리 ID 집합", example = "[10,201,502]")
    Set<Long> summarizedStoryIdSet,
    @Schema(description = "수정 시각", example = "[2025,11,25,06,00,01,823454000]")
    LocalDateTime updatedAt
) {
    public static StorySummaryResponse toDto(StorySpotSummary summary) {
        return new StorySummaryResponse(
            summary.getId(),
            summary.getStoryConcept(),
            summary.getDocentUrl(),
            summary.getTitle(),
            summary.getSummary(),
            summary.getSummarizedStoryIdSet(),
            summary.getUpdatedAt()
        );
    }
}
