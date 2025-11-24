package com.earseo.story.controller;

import com.earseo.story.common.BaseResponse;
import com.earseo.story.dto.response.SpotTitleListResponse;
import com.earseo.story.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/story")
@RequiredArgsConstructor
public class StoryController {
    private final StoryService storyService;

    @Operation(
        summary = "이야기 스팟의 상위 이야기 제목 목록 조회",
        description = """
            특정 이야기 스팟에서 가장 많이 사용된 이야기 제목 목록을 조회합니다.
            - 이야기 개수의 내림차순으로 정렬됩니다.
            - 이야기 개수가 동일한 경우 최근에 게시글이 생성된 순으로 정렬됩니다.
            - 최대 4개의 이야기 제목을 반환합니다.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "이야기 제목 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = SpotTitleListResponse.class))
        )
    })
    @GetMapping("/spot/{storySpotId}/name")
    public ResponseEntity<BaseResponse<SpotTitleListResponse>> getSpotTitleList(
        @Parameter(description = "이야기 스팟 ID", required = true, example = "1")
        @PathVariable Long storySpotId
    ) {
        return ResponseEntity.ok(BaseResponse.ok(storyService.getSpotTitleList(storySpotId)));
    }
}
