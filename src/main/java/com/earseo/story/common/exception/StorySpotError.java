package com.earseo.story.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StorySpotError implements ErrorCodeInterface {

    STORY_SPOT_ERROR("STS001", "스토리 스팟에러를 입력해주세요.", HttpStatus.I_AM_A_TEAPOT),
    ;

    private final String status;
    private final String message;
    private final HttpStatus httpStatus;

    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.builder()
                .status(status)
                .message(message)
                .httpStatus(httpStatus)
                .build();
    }
}
