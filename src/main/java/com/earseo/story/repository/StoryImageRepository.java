package com.earseo.story.repository;

import com.earseo.story.entity.StoryImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StoryImageRepository extends JpaRepository<StoryImage, Long> {

    @Query("SELECT si FROM StoryImage si WHERE si.story.id IN :storyIds ORDER BY si.story.id, si.createdAt")
    List<StoryImage> findByStoryIdIn(@Param("storyIds") List<Long> storyIds);

    // 수정 시 이미지 확인용
    List<StoryImage> findByStoryId(Long storyId);

    // keepUrls에 없는 이미지 삭제 -> 남길 것만 명시하면 나머지는 자동으로 삭제 처리
    @Modifying
    @Query("DELETE FROM StoryImage si WHERE si.story.id = :storyId AND si.imageUrl NOT IN :keepUrls")
    void deleteByStoryIdAndImageUrlNotIn(@Param("storyId") Long storyId,
                                         @Param("keepUrls") List<String> keepUrls);
}
