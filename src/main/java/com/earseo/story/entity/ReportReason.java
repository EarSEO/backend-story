package com.earseo.story.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportReason {
    ADVERTISEMENT("영리목적/홍보성"),
    COPYRIGHT("저작권침해"),
    SEXUAL("음란성/선정성"),
    INSULT("욕설/인신공격"),
    PRIVACY("개인정보노출"),
    DUPLICATE("같은내용 반복게시"),
    ETC("기타");

    private final String description;
}