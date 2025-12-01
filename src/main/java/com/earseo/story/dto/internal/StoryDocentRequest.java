package com.earseo.story.dto.internal;

public record StoryDocentRequest(
        Long summaryId,
        String summary,
        String locale
) {
}
