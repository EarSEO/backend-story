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
public class StoryImage {
    @Id
    @Column(name = "story_image_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JoinColumn(name = "story_id", nullable = false)
    @ManyToOne
    private Story story;
    @Column(nullable = false, updatable = false)
    private String imageUrl;
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
