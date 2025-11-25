package com.earseo.story.repository;

import com.earseo.story.entity.StoryAuthor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryAuthorRepository extends JpaRepository<StoryAuthor, Long> {
}
