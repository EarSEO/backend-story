package com.earseo.story.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class StorySpot {
    @Id
    @Column(name = "story_spot_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition = "geometry(Point, 4326)", nullable = false, updatable = false)
    private Point center;
    @Column(columnDefinition = "varchar(12)", nullable = false, updatable = false)
    private String geohash;
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
