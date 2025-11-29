package com.earseo.story.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
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
                name = "story_like_unique",
                columnNames = {"story_id", "member_id"}
        ),
        indexes = @Index(name = "idx_story_like_story", columnList = "story_id")
)
public class StoryLike {
    @Id
    @Column(name = "story_like_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}