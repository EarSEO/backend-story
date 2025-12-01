package com.earseo.story.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(
    uniqueConstraints = {@UniqueConstraint(
        name = "spot_concept_locale_constraint",
        columnNames = {"story_spot_id", "story_concept", "locale"}
    )},
    indexes = {
        @Index(name = "spot_concept_locale_idx", columnList = "story_spot_id,story_concept,locale,updated_at DESC"),
    }
)
public class StorySpotSummary {
    @Id
    @Column(name = "story_spot_summary_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JoinColumn(name = "story_spot_id", nullable = false)
    @ManyToOne
    private StorySpot storySpot;
    @Enumerated(EnumType.STRING)
    @Column(name = "story_concept")
    private StoryConcept storyConcept;
    private String docentUrl;
    @Column(columnDefinition = "text")
    private String docentScript;
    @Enumerated(EnumType.STRING)
    @Column(name = "locale")
    private Locale locale;
    private String title;
    @Column(columnDefinition = "text")
    private String summary;
    @Type(JsonType.class)
    @Column(name = "summarized_story_id_set", columnDefinition = "JSONB", nullable = false)
    private Set<Long> summarizedStoryIdSet;
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public StorySpotSummary updateDocent(String docentUrl, String docentScript){
        this.docentUrl = docentUrl;
        this.docentScript = docentScript;
        return this;
    }
}
