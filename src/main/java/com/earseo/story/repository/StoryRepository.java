package com.earseo.story.repository;

import com.earseo.story.entity.Story;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoryRepository extends JpaRepository<Story, Long> {
    @EntityGraph(attributePaths = {"storyAuthor", "storyTitle", "storySpot"})
    @Query("""
        SELECT s FROM Story s
        WHERE s.storySpot.id = :storySpotId
        """)
    Slice<Story> findByStorySpotIdAndLocale(
        @Param("storySpotId") Long storySpotId,
        Pageable pageable
    );

    // 첫 조회 (lastStoryId 없을 때)
    @EntityGraph(attributePaths = {"storySpot", "storyTitle", "storyAuthor"})
    @Query("SELECT s FROM Story s WHERE s.storyAuthor.id = :authorId ORDER BY s.id DESC")
    List<Story> findByStoryAuthorIdOrderByIdDesc(@Param("authorId") Long authorId,
                                                 org.springframework.data.domain.Pageable pageable);

    // 이후 조회 (lastStoryId 있을 때)
    @EntityGraph(attributePaths = {"storySpot", "storyTitle", "storyAuthor"})
    @Query("SELECT s FROM Story s WHERE s.storyAuthor.id = :authorId AND s.id < :lastStoryId ORDER BY s.id DESC")
    List<Story> findByStoryAuthorIdAndIdLessThanOrderByIdDesc(@Param("authorId") Long authorId,
                                                              @Param("lastStoryId") Long lastStoryId,
                                                              org.springframework.data.domain.Pageable pageable);
}
