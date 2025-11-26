package com.earseo.story.controller;

import com.earseo.story.common.BaseResponse;
import com.earseo.story.common.exception.BaseException;
import com.earseo.story.dto.request.CreateRequest;
import com.earseo.story.dto.response.CreateResponse;
import com.earseo.story.dto.response.MyStoryListResponse;
import com.earseo.story.service.StoryService;
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
        summary = "이야기 생성",
        description = """
            사용자가 새로운 이야기를 작성합니다.
            - 위치 정보(위도/경도)를 기반으로 GeoHash를 생성하여 이야기 스팟에 저장합니다.
            - 최대 3개의 이미지를 함께 업로드할 수 있습니다.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "이야기 생성 성공",
            content = @Content(schema = @Schema(implementation = CreateResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = {
                    @ExampleObject(
                        name = "이미지 개수 초과",
                        value = """
                            {
                                "status": "STR001",
                                "message": "이야기의 사진은 3개 이하여야 합니다.",
                                "data": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "이미지 업로드 실패",
                        value = """
                            {
                                "status": "STR003",
                                "message": "이미지 업로드 중 오류가 발생했습니다.",
                                "data": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "유효성 검증 실패",
                        value = """
                            {
                              "status": "ARGUMENT_ERROR",
                              "message": "경도는 124 이상이어야 합니다",
                              "data": null
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "인증 오류",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "사용자 불일치",
                    value = """
                        {
                            "status": "STR002",
                            "message": "jwt 사용자와 입력 정보가 불일치 합니다.",
                            "data": null
                        }
                        """
                )
            )
        )
    })
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<CreateResponse>> createStory(
        @Parameter(description = "사용자 ID", required = true)
        @RequestHeader("X-USER-ID") Long memberId,
        @Parameter(
            description = "이야기 생성 요청 바디",
            required = true,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
        @Valid @RequestPart CreateRequest body,
        @Parameter(
            description = "이야기 이미지 파일 (최대 3개)",
            required = false,
            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
        )
        @RequestPart(required = false) List<MultipartFile> images
    ) {
        if (images != null && images.size() > 3) throw new BaseException(STORY_IMAGE_TOO_MANY);
        return ResponseEntity.ok(BaseResponse.ok(storyService.createStory(memberId, body, images)));
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
}
