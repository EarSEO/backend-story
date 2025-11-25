package com.earseo.story.controller;

import com.earseo.story.common.BaseResponse;
import com.earseo.story.dto.response.LocationSpotBriefInfoResponse;
import com.earseo.story.dto.response.MapSpotInfoList;
import com.earseo.story.dto.response.SearchSpotInfoList;
import com.earseo.story.dto.response.SpotTitleListResponse;
import com.earseo.story.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.*;
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

    @Operation(
        summary = "지도 사각형 영역 내 이야기 스팟 조회",
        description = """
            지도에서 사각형 영역 내의 이야기 스팟 목록을 조회합니다.
            - 좌측 하단(minLongitude, minLatitude)과 우측 상단(maxLongitude, maxLatitude) 좌표를 받습니다.
            - 해당 영역 내에 포함된 모든 이야기 스팟의 위치 정보를 반환합니다.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = MapSpotInfoList.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = {
                    @ExampleObject(
                        name = "좌표 범위 오류",
                        value = """
                            {
                                "status": "ARGUMENT_ERROR",
                                "message": "최소 위도/경도는 최대 위도/경도보다 작아야 합니다.",
                                "data": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "경도 범위 초과",
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
        )
    })
    @GetMapping("/spot/map/rectangle")
    public ResponseEntity<BaseResponse<MapSpotInfoList>> getRectangleMapInfoList(
        @Parameter(
            description = "사각형 영역의 최소 경도 (좌측 하단)",
            required = true,
            example = "126.9000",
            schema = @Schema(minimum = "124", maximum = "133")
        )
        @RequestParam
        @NotNull(message = "최소 경도는 필수입니다")
        @DecimalMin(value = "124", message = "경도는 124 이상이어야 합니다")
        @DecimalMax(value = "133", message = "경도는 133 이하여야 합니다")
        Double minLongitude,

        @Parameter(
            description = "사각형 영역의 최소 위도 (좌측 하단)",
            required = true,
            example = "37.5000",
            schema = @Schema(minimum = "33.0", maximum = "39")
        )
        @RequestParam
        @NotNull(message = "최소 위도는 필수입니다")
        @DecimalMin(value = "33.0", message = "위도는 33 이상이어야 합니다")
        @DecimalMax(value = "39", message = "위도는 39 이하여야 합니다")
        Double minLatitude,

        @Parameter(
            description = "사각형 영역의 최대 경도 (우측 상단)",
            required = true,
            example = "127.1000",
            schema = @Schema(minimum = "124", maximum = "133")
        )
        @RequestParam
        @NotNull(message = "최대 경도는 필수입니다")
        @DecimalMin(value = "124", message = "경도는 124 이상이어야 합니다")
        @DecimalMax(value = "133", message = "경도는 133 이하여야 합니다")
        Double maxLongitude,

        @Parameter(
            description = "사각형 영역의 최대 위도 (우측 상단)",
            required = true,
            example = "37.6000",
            schema = @Schema(minimum = "33.0", maximum = "39")
        )
        @RequestParam
        @NotNull(message = "최대 위도는 필수입니다")
        @DecimalMin(value = "33.0", message = "위도는 33 이상이어야 합니다")
        @DecimalMax(value = "39", message = "위도는 39 이하여야 합니다")
        Double maxLatitude
    ) {
        return ResponseEntity.ok(BaseResponse.ok(
            storyService.getRectangleMapInfoList(minLongitude, minLatitude, maxLongitude, maxLatitude)
        ));
    }

    @Operation(
        summary = "특정 지점 반경 내 이야기 스팟 조회",
        description = """
            특정 좌표를 중심으로 지정한 반경(미터) 내의 이야기 스팟 목록을 조회합니다.
            - 중심 좌표와 반경을 입력받습니다.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = MapSpotInfoList.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = {
                    @ExampleObject(
                        name = "반경 범위 오류",
                        value = """
                            {
                                "status": "ARGUMENT_ERROR",
                                "message": "반경은 1 이상이어야 합니다",
                                "data": null
                            }
                            """
                    ),
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
                        name = "최대 반경 초과",
                        value = """
                            {
                                "status": "ARGUMENT_ERROR",
                                "message": "반경은 30000 이하여야 합니다",
                                "data": null
                            }
                            """
                    )
                }
            )
        )
    })
    @GetMapping("/spot/map/circle")
    public ResponseEntity<BaseResponse<MapSpotInfoList>> getCircleMapInfo(
        @Parameter(
            description = "검색 반경 (미터 단위)",
            required = true,
            example = "1000",
            schema = @Schema(minimum = "1", maximum = "30000")
        )
        @RequestParam
        @NotNull(message = "반경은 필수입니다")
        @DecimalMin(value = "1", message = "반경은 1 이상이어야 합니다")
        @DecimalMax(value = "30000", message = "반경은 30000 이하여야 합니다")
        Double meters,

        @Parameter(
            description = "중심점 경도",
            required = true,
            example = "126.9780",
            schema = @Schema(minimum = "124", maximum = "133")
        )
        @RequestParam
        @NotNull(message = "경도는 필수입니다")
        @DecimalMin(value = "124", message = "경도는 124 이상이어야 합니다")
        @DecimalMax(value = "133", message = "경도는 133 이하여야 합니다")
        Double longitude,

        @Parameter(
            description = "중심점 위도",
            required = true,
            example = "37.5665",
            schema = @Schema(minimum = "33.0", maximum = "39")
        )
        @RequestParam
        @NotNull(message = "위도는 필수입니다")
        @DecimalMin(value = "33.0", message = "위도는 33 이상이어야 합니다")
        @DecimalMax(value = "39", message = "위도는 39 이하여야 합니다")
        Double latitude
    ) {
        return ResponseEntity.ok(BaseResponse.ok(
            storyService.getCircleMapInfo(meters, longitude, latitude)
        ));
    }

    @Operation(
        summary = "이야기 제목 키워드 검색 (사각 영역)",
        description = """
            지정된 사각형 영역 내에서 키워드를 포함하는 이야기 제목을 가진 스팟을 검색합니다.
            - 각 스팟의 상위 4개 이야기 제목 중 키워드가 포함된 스팟을 반환합니다.
            - 요청 좌표로부터의 거리를 함께 반환합니다.
            - 키워드와 매칭된 제목 중 이야기 개수가 가장 많은 제목을 반환합니다.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = SearchSpotInfoList.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = {
                    @ExampleObject(
                        name = "키워드 누락",
                        value = """
                            {
                                "status": "ARGUMENT_ERROR",
                                "message": "검색 키워드는 필수입니다",
                                "data": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "키워드 길이 초과",
                        value = """
                            {
                                "status": "ARGUMENT_ERROR",
                                "message": "검색 키워드는 20자 이하여야 합니다",
                                "data": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "좌표 범위 오류",
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
        )
    })
    @GetMapping("/search/title")
    public ResponseEntity<BaseResponse<SearchSpotInfoList>> searchTitle(
        @Parameter(
            description = "검색 키워드",
            required = true,
            example = "맛집"
        )
        @RequestParam
        @NotBlank(message = "검색 키워드는 필수입니다")
        @Size(max = 20, message = "검색 키워드는 20자 이하여야 합니다")
        String keyword,

        @Parameter(
            description = "기준 경도",
            required = true,
            example = "126.9780",
            schema = @Schema(minimum = "124", maximum = "133")
        )
        @RequestParam
        @NotNull(message = "경도는 필수입니다")
        @DecimalMin(value = "124", message = "경도는 124 이상이어야 합니다")
        @DecimalMax(value = "133", message = "경도는 133 이하여야 합니다")
        Double longitude,

        @Parameter(
            description = "기준 위도",
            required = true,
            example = "37.5665",
            schema = @Schema(minimum = "33.0", maximum = "39")
        )
        @RequestParam
        @NotNull(message = "위도는 필수입니다")
        @DecimalMin(value = "33.0", message = "위도는 33 이상이어야 합니다")
        @DecimalMax(value = "39", message = "위도는 39 이하여야 합니다")
        Double latitude,

        @Parameter(
            description = "사각형 영역의 최소 경도 (좌측 하단)",
            required = true,
            example = "126.9000",
            schema = @Schema(minimum = "124", maximum = "133")
        )
        @RequestParam
        @NotNull(message = "최소 경도는 필수입니다")
        @DecimalMin(value = "124", message = "경도는 124 이상이어야 합니다")
        @DecimalMax(value = "133", message = "경도는 133 이하여야 합니다")
        Double minLongitude,

        @Parameter(
            description = "사각형 영역의 최소 위도 (좌측 하단)",
            required = true,
            example = "37.5000",
            schema = @Schema(minimum = "33.0", maximum = "39")
        )
        @RequestParam
        @NotNull(message = "최소 위도는 필수입니다")
        @DecimalMin(value = "33.0", message = "위도는 33 이상이어야 합니다")
        @DecimalMax(value = "39", message = "위도는 39 이하여야 합니다")
        Double minLatitude,

        @Parameter(
            description = "사각형 영역의 최대 경도 (우측 상단)",
            required = true,
            example = "127.1000",
            schema = @Schema(minimum = "124", maximum = "133")
        )
        @RequestParam
        @NotNull(message = "최대 경도는 필수입니다")
        @DecimalMin(value = "124", message = "경도는 124 이상이어야 합니다")
        @DecimalMax(value = "133", message = "경도는 133 이하여야 합니다")
        Double maxLongitude,

        @Parameter(
            description = "사각형 영역의 최대 위도 (우측 상단)",
            required = true,
            example = "37.6000",
            schema = @Schema(minimum = "33.0", maximum = "39")
        )
        @RequestParam
        @NotNull(message = "최대 위도는 필수입니다")
        @DecimalMin(value = "33.0", message = "위도는 33 이상이어야 합니다")
        @DecimalMax(value = "39", message = "위도는 39 이하여야 합니다")
        Double maxLatitude,


        @Parameter(
            description = "반환할 최대 결과 개수",
            example = "10",
            schema = @Schema(minimum = "1", maximum = "100")
        )
        @RequestParam(defaultValue = "10")
        @Min(value = 1, message = "제한 개수는 1 이상이어야 합니다")
        @Max(value = 100, message = "제한 개수는 100 이하여야 합니다")
        Integer limit
    ) {
        return ResponseEntity.ok(BaseResponse.ok(
            storyService.searchTitleRectangle(keyword, longitude, latitude, minLongitude, minLatitude, maxLongitude, maxLatitude, limit)
        ));
    }

}
