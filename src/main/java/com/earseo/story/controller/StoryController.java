package com.earseo.story.controller;

import com.earseo.story.common.BaseResponse;
import com.earseo.story.dto.response.LocationSpotBriefInfoResponse;
import com.earseo.story.dto.response.SpotTitleListResponse;
import com.earseo.story.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
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
            - 빈 배열이 반환된다면 없는 이야기 스팟 id로 요청한 것입니다.
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

    @Operation(
        summary = "좌표 기반 이야기 스팟 정보 조회",
        description = """
            위도/경도 좌표를 기반으로 해당 위치의 이야기 스팟 정보를 조회합니다.
            - 좌표를 GeoHash로 변환하여 이야기 스팟을 검색합니다.
            - 스팟이 존재하는 경우, 해당 스팟의 상위 이야기 제목 목록(최대 4개)을 반환합니다.
            - 스팟이 존재하지 않는 경우, spotId는 null이고 빈 목록을 반환합니다.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = LocationSpotBriefInfoResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = {
                    @ExampleObject(
                        name = "경도 범위 초과",
                        value = """
                            {
                                "status": "ARGUMENT_ERROR",
                                "message": "경도는 124 이상이어야 합니다",
                                "data": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "위도 범위 초과",
                        value = """
                            {
                                "status": "ARGUMENT_ERROR",
                                "message": "위도는 33 이상이어야 합니다",
                                "data": null
                            }
                            """
                    ),
                }
            )
        )
    })
    @GetMapping("/spot/info/brief")
    public ResponseEntity<BaseResponse<LocationSpotBriefInfoResponse>> getLocationSpotBriefInfo(
        @Parameter(
            description = "경도",
            required = true,
            example = "126.9780",
            schema = @Schema(minimum = "124", maximum = "133")
        )
        @NotNull(message = "경도는 필수입니다")
        @DecimalMin(value = "124", message = "경도는 124 이상이어야 합니다")
        @DecimalMax(value = "133", message = "경도는 133 이하여야 합니다")
        @RequestParam Double longitude,

        @Parameter(
            description = "위도",
            required = true,
            example = "37.5665",
            schema = @Schema(minimum = "33.0", maximum = "39")
        )
        @NotNull(message = "위도는 필수입니다")
        @DecimalMin(value = "33.0", message = "위도는 33 이상이어야 합니다")
        @DecimalMax(value = "39", message = "위도는 39 이하여야 합니다")
        @RequestParam Double latitude
    ) {
        return ResponseEntity.ok(BaseResponse.ok(storyService.getLocationSpotBriefInfo(longitude, latitude)));
    }
}
