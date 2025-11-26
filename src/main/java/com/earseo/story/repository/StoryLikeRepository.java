package com.earseo.story.repository;

import com.earseo.story.entity.StoryLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoryLikeRepository extends JpaRepository<StoryLike, Long> {

    Optional<StoryLike> findByStoryIdAndMemberId(Long storyId, Long memberId);

    boolean existsByStoryIdAndMemberId(Long storyId, Long memberId);
}