package com.earseo.story.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(
    uniqueConstraints = @UniqueConstraint(
        name = "spot_title_unique_constraint",
        columnNames = {"story_spot_id", "story_title_id"}
    ),
    indexes = {
        @Index(name = "spot_title_aggregate_spot_idx", columnList = "story_spot_id,story_count DESC"),
        @Index(name = "spot_title_aggregate_title_idx", columnList = "story_title_id,story_count DESC")
    })
public class SpotTitleAggregate {
    @Id
    @Column(name = "spot_title_aggregate_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_spot_id", nullable = false, updatable = false)
    private StorySpot storySpot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_title_id", nullable = false, updatable = false)
    private StoryTitle storyTitle;

    @Builder.Default
    @Column(name = "story_count", nullable = false)
    private Long storyCount = 0L;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
