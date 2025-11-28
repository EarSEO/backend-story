package com.earseo.story.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
public class StoryPageRequest {
    @Schema(description = "페이지 번호 (0부터 시작)", example = "0", defaultValue = "0")
    private int page = 0;

    @Schema(description = "페이지 크기", example = "10", defaultValue = "10")
    private int size = 10;

    @Schema(
        description = "정렬 기준",
        example = "createdAt,desc",
        allowableValues = {"createdAt,desc", "createdAt,asc", "likeCount,desc", "likeCount,asc"}
    )
    private String sort = "createdAt,desc";

    public Pageable toPageable() {
        String[] sortParts = sort.split(",");
        String property = sortParts[0];
        Sort.Direction direction = sortParts.length > 1
            ? Sort.Direction.fromString(sortParts[1])
            : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, property));
    }
}
