package com.earseo.story.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.geo.Point;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Story {
    @Id
    @Column(name = "story_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JoinColumn(name = "story_spot_id", nullable = false)
    @ManyToOne
    private StorySpot storySpot;
    @JoinColumn(name = "story_author_id")
    @ManyToOne
    private StoryAuthor storyAuthor;
    private Point point;
    private String title;
    @Column(columnDefinition = "text")
    private String content;
    @Enumerated(EnumType.STRING)
    private Locale locale;
    @Enumerated(EnumType.STRING)
    private StoryConcept storyConcept;
    private Long likeCount; // 리팩토링해서 뺴내야함
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
