package com.earseo.story.controller;

import com.earseo.story.common.BaseResponse;
import com.earseo.story.dto.request.GetRouteListSpotRequest;
import com.earseo.story.dto.response.GetRouteListSpotResponse;
import com.earseo.story.service.StorySpotSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/internal/story")
@RequiredArgsConstructor
public class InternalStoryController {
    private final StorySpotSummaryService storySpotSummaryService;

    @Operation(
        summary = "경로 주변 이야기 스팟 조회 (내부 API)",
        description = """
            여러 경로(LineString)를 입력받아 각 경로 주변의 요약이 있는 이야기 스팟을 조회합니다.
            
            **기능**
            - 각 경로마다 지정된 거리(meters) 내의 스팟을 검색합니다
            - 경로당 최대 개수(amount)만큼 스팟을 반환합니다
            - 이야기 개수가 많은 스팟을 우선적으로 반환합니다
            - storyConcept가 null이면 랜덤하게 선택합니다
            
            **반환값**
            - 요약이 없는 경로는 해당 인덱스에 null로 반환됩니다
            - 결과는 요청한 경로 순서와 동일하게 반환됩니다
            - 각 경로별로 이야기 개수가 많은 순서로 정렬됩니다
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = GetRouteListSpotResponse.class),
                examples = @ExampleObject(
                    name = "성공 응답 예시",
                    value = """
                        {
                            "status": "SUCCESS",
                            "message": "요청이 성공적으로 처리되었습니다",
                            "data": {
                                "spotList": [
                                    [
                                        {
                                            "longitude": 126.9780,
                                            "latitude": 37.5665,
                                            "storySpotId": 1,
                                            "summaryId": 10,
                                            "title": "서울 시청",
                                            "summaryTitle": "서울 시청 주변 맛집 완전 정복",
                                            "storyConcept": "TIP",
                                            "docentUrl": "https://cdn.example.com/docent/1.mp3"
                                        },
                                        {
                                            "longitude": 126.9790,
                                            "latitude": 37.5670,
                                            "storySpotId": 2,
                                            "summaryId": 11,
                                            "title": "시청 우체국",
                                            "summaryTitle": "숨은 카페 명소",
                                            "storyConcept": "TIP",
                                            "docentUrl": "https://cdn.example.com/docent/2.mp3"
                                        }
                                    ],
                                    null,
                                    [
                                        {
                                            "longitude": 127.0276,
                                            "latitude": 37.4979,
                                            "storySpotId": 5,
                                            "summaryId": 20,
                                            "title": "강남역 4번출구",
                                            "summaryTitle": "강남의 역사적 변천",
                                            "storyConcept": "HISTORY",
                                            "docentUrl": "https://cdn.example.com/docent/5.mp3"
                                        }
                                    ]
                                ]
                            }
                        }
                        """
                )
            )
        ),
    })
    @PostMapping("/path/spots")
    public ResponseEntity<BaseResponse<GetRouteListSpotResponse>> getPathsSpotList(
        @RequestBody @Valid
        GetRouteListSpotRequest request
    ) {
        return ResponseEntity.ok(BaseResponse.ok(storySpotSummaryService.getPathsSpotList(request)));
    }
}
