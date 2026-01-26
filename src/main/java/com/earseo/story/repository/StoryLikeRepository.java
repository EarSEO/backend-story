package com.earseo.story.repository;

import com.earseo.story.entity.StoryLike;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoryLikeRepository extends JpaRepository<StoryLike, Long> {

    Optional<StoryLike> findByStoryIdAndMemberId(Long storyId, Long memberId);

    boolean existsByStoryIdAndMemberId(Long storyId, Long memberId);

    @Query("""
        SELECT sl FROM StoryLike sl
        JOIN FETCH sl.story s
        JOIN FETCH s.storyAuthor
        JOIN FETCH s.storyTitle
        JOIN FETCH s.storySpot
        WHERE sl.memberId = :memberId
        ORDER BY sl.id DESC
        """)
    List<StoryLike> findByMemberIdOrderByIdDesc(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    @Query("""
        SELECT sl FROM StoryLike sl
        JOIN FETCH sl.story s
        JOIN FETCH s.storyAuthor
        JOIN FETCH s.storyTitle
        JOIN FETCH s.storySpot
        WHERE sl.memberId = :memberId AND sl.id < :lastStoryLikeId
        ORDER BY sl.id DESC
        """)
    List<StoryLike> findByMemberIdAndIdLessThanOrderByIdDesc(
            @Param("memberId") Long memberId,
            @Param("lastStoryLikeId") Long lastStoryLikeId,
            Pageable pageable
    );
}