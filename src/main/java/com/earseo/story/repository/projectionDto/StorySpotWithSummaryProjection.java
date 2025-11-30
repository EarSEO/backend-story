package com.earseo.story.repository.projectionDto;

import com.earseo.story.entity.StoryConcept;
import java.time.LocalDateTime;

public interface StorySpotWithSummaryProjection {
    Long getStorySpotId();
    Double getLongitude();
    Double getLatitude();
    LocalDateTime getCreatedAt();
    String getTopTitle();
    Long getSummaryId();
    String getSummaryTitle();
    String getSummary();
    StoryConcept getStoryConcept();
    String getDocentUrl();
}
