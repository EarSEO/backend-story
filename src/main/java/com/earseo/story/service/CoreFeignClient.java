package com.earseo.story.service;

import com.earseo.story.dto.internal.StoryDocentRequest;
import com.earseo.story.dto.internal.StoryDocentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "CoreFeignClient"
)
public interface CoreFeignClient {

    @PostMapping("/internal/core/docent/story")
    List<StoryDocentResponse> getStoryDocent(@RequestBody List<StoryDocentRequest> request);
}
