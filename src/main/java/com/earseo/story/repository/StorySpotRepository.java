package com.earseo.story.repository;

import com.earseo.story.entity.StorySpot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StorySpotRepository extends JpaRepository<StorySpot, Long> {
    Optional<StorySpot> findByGeohash(String geohash);
}
