package com.earseo.story.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StorySpotError implements ErrorCodeInterface {

    STORY_SPOT_NOT_FOUND("STS001", "없는 스토리 스팟입니다.", HttpStatus.NOT_FOUND),
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
