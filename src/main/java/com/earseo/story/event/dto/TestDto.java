package com.earseo.story.event.dto;

import java.time.LocalDateTime;

public record TestDto(
    String hello,
    Integer world,
    LocalDateTime date
) {
}
