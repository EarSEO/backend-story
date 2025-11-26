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
    INVALID_COORDINATE_RANGE("STR004", "최소 위도/경도는 최대 위도/경도보다 작아야 합니다.", HttpStatus.BAD_REQUEST),
    STORY_NOT_FOUND("STR005", "이야기를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    STORY_NOT_OWNER("STR006", "이야기 작성자만 수정/삭제할 수 있습니다.", HttpStatus.FORBIDDEN),
    STORY_ALREADY_REPORTED("STR007", "이미 신고한 이야기입니다.", HttpStatus.CONFLICT),
    STORY_ALREADY_LIKED("STR008", "이미 좋아요한 이야기입니다.", HttpStatus.CONFLICT),
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
