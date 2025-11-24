package com.earseo.story.controller;

import com.earseo.story.common.BaseResponse;
import com.earseo.story.common.exception.BaseException;
import com.earseo.story.dto.request.CreateRequest;
import com.earseo.story.dto.response.CreateResponse;
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
}
