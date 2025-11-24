package com.earseo.story.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StoryAuthor {
    @Id
    @Column(name = "story_author_id")
    private Long id;
    private String nickname;
    private String profileUrl;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public StoryAuthor updateAuthor(String nickname, String profileUrl, LocalDateTime updatedAt) {
        this.nickname = nickname;
        this.profileUrl = profileUrl;
        this.updatedAt = updatedAt;
        return this;
    }
}
