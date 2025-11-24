package com.earseo.story.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StorySpotSummary {
    @Id
    @Column(name = "story_spot_summary_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JoinColumn(name = "story_spot_id", nullable = false)
    @ManyToOne
    private StorySpot storySpot;
    @Enumerated(EnumType.STRING)
    private StoryConcept storyConcept;
    private String docentUrl;
    @Enumerated(EnumType.STRING)
    private Locale locale;
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
