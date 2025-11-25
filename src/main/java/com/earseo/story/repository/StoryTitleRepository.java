package com.earseo.story.repository;

import com.earseo.story.entity.StoryTitle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoryTitleRepository extends JpaRepository<StoryTitle, Long> {
    Optional<StoryTitle> findByTitle(String title);
}
