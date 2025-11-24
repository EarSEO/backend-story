package com.earseo.story.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StoryError implements ErrorCodeInterface {
    STORY_IMAGE_TOO_MANY("STR001", "이야기의 사진은 3개 이하여야 합니다.", HttpStatus.BAD_REQUEST),
    ITS_NOT_YOU("STR002", "jwt 사용자와 입력 정보가 불일치 합니다.", HttpStatus.CONFLICT),
    STORY_IMAGE_UPLOAD_FAILED("STR003", "이미지 업로드 중 오류가 발생했습니다.", HttpStatus.BAD_REQUEST),
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
