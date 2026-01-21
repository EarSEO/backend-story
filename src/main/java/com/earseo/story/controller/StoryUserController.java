package com.earseo.story.controller;

import com.earseo.story.common.BaseResponse;
import com.earseo.story.common.exception.BaseException;
import com.earseo.story.dto.request.CreateRequest;
import com.earseo.story.dto.request.UpdateStoryRequest;
import com.earseo.story.dto.response.CreateResponse;
import com.earseo.story.dto.response.MyStoryListResponse;
import com.earseo.story.dto.response.ToggleLikeResponse;
import com.earseo.story.dto.response.UpdateStoryResponse;
import com.earseo.story.service.StoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.earseo.story.common.exception.StoryError.STORY_IMAGE_TOO_MANY;

@RestController
@RequestMapping("/api/user/story")
@RequiredArgsConstructor
public class StoryUserController {
    private final StoryService storyService;

    @Operation(
            summary = "이야기 생성 (이미지 포함)",
            description = """
                사용자가 새로운 이야기를 작성합니다.
                - 최대 3개의 이미지를 함께 업로드할 수 있습니다.
                """
    )
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<CreateResponse>> createStory(
            @RequestHeader("X-USER-ID") Long memberId,
            @RequestPart("body") String body,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        CreateRequest request = objectMapper.readValue(body, CreateRequest.class);

        if (images != null && images.size() > 3) {
            throw new BaseException(STORY_IMAGE_TOO_MANY);
        }
        return ResponseEntity.ok(BaseResponse.ok(storyService.createStory(memberId, request, images)));
    }

    @Operation(
            summary = "나의 이야기 목록 조회",
            description = """
                    로그인한 사용자가 작성한 이야기 목록을 최신순으로 조회합니다.
                    - 무한 스크롤 지원 (Cursor 기반 페이징)
                    - 첫 조회: lastStoryId 없이 호출
                    - 이후 조회: 이전 응답의 lastStoryId를 파라미터로 전달
                    - 이미지, 좋아요 개수, 위치 정보 포함
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MyStoryListResponse.class))
            )
    })
    @GetMapping("/my")
    public ResponseEntity<BaseResponse<MyStoryListResponse>> getMyStories(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-USER-ID") Long memberId,

            @Parameter(description = "마지막 이야기 ID (첫 조회 시 생략)", example = "42")
            @RequestParam(required = false) Long lastStoryId,

            @Parameter(description = "조회할 개수", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(BaseResponse.ok(storyService.getMyStories(memberId, lastStoryId, size)));
    }

    @Operation(
            summary = "이야기 좋아요 토글",
            description = """
                    이야기에 좋아요를 추가하거나 취소합니다.
                    - 좋아요가 없으면 추가, 있으면 취소
                    - Redis 캐싱으로 성능 최적화
                    - 응답에 현재 좋아요 상태와 총 개수 포함
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "좋아요 토글 성공",
                    content = @Content(schema = @Schema(implementation = ToggleLikeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "이야기를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "status": "STR005",
                                                "message": "이야기를 찾을 수 없습니다.",
                                                "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/{storyId}/like")
    public ResponseEntity<BaseResponse<ToggleLikeResponse>> toggleLike(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-USER-ID") Long memberId,

            @Parameter(description = "좋아요할 이야기 ID", required = true)
            @PathVariable Long storyId
    ) {
        return ResponseEntity.ok(BaseResponse.ok(storyService.toggleLike(storyId, memberId)));
    }

    @Operation(
            summary = "이야기 수정",
            description = """
                    작성한 이야기를 수정합니다.
                    - 작성자 본인만 수정 가능
                    - 제목, 내용, 컨셉, 이미지 수정 가능
                    - 이미지는 keepImageUrls로 유지할 이미지를 지정하고, files로 새 이미지 추가
                    - 최종 이미지는 최대 3개까지 가능
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = UpdateStoryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "status": "STR006",
                                                "message": "이야기 작성자만 수정/삭제할 수 있습니다.",
                                                "data": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "이야기를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "status": "STR005",
                                                "message": "이야기를 찾을 수 없습니다.",
                                                "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    @PatchMapping(value = "/{storyId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<UpdateStoryResponse>> updateStory(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-USER-ID") Long memberId,

            @Parameter(description = "수정할 이야기 ID", required = true)
            @PathVariable Long storyId,

            @Parameter(
                    description = "이야기 수정 요청 바디",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
            @Valid @RequestPart UpdateStoryRequest body,

            @Parameter(
                    description = "새로 추가할 이미지 파일 (최대 3개)",
                    required = false,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart(required = false) List<MultipartFile> files
    ) {
        return ResponseEntity.ok(BaseResponse.ok(storyService.updateStory(storyId, memberId, body, files)));
    }

    @Operation(
            summary = "좋아요한 이야기 목록 조회",
            description = """
                로그인한 사용자가 좋아요한 이야기 목록을 최신순으로 조회합니다.
                - 무한 스크롤 지원 (Cursor 기반 페이징)
                - 첫 조회: lastStoryLikeId 없이 호출
                - 이후 조회: 이전 응답의 lastStoryId(실제로는 storyLikeId)를 파라미터로 전달
                """
    )
    @GetMapping("/liked")
    public ResponseEntity<BaseResponse<MyStoryListResponse>> getLikedStories(
            @RequestHeader("X-USER-ID") Long memberId,

            @Parameter(description = "마지막 좋아요 ID (첫 조회 시 생략)")
            @RequestParam(required = false) Long lastStoryLikeId,

            @Parameter(description = "조회할 개수", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(BaseResponse.ok(storyService.getLikedStories(memberId, lastStoryLikeId, size)));
    }
}
