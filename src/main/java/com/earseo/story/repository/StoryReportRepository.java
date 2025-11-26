package com.earseo.story.repository;

import com.earseo.story.entity.StoryReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryReportRepository extends JpaRepository<StoryReport, Long> {
}