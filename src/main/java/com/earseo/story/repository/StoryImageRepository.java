package com.earseo.story.repository;

import com.earseo.story.entity.StoryImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StoryImageRepository extends JpaRepository<StoryImage, Long> {

    @Query("SELECT si FROM StoryImage si WHERE si.story.id IN :storyIds ORDER BY si.story.id, si.createdAt")
    List<StoryImage> findByStoryIdIn(@Param("storyIds") List<Long> storyIds);
}
