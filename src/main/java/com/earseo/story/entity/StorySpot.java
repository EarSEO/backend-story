package com.earseo.story.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StorySpot {
    @Id
    @Column(name = "story_spot_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
