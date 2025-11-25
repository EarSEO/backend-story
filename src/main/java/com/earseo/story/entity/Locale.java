package com.earseo.story.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Locale {
    KO("한국어", "Korean"),
    EN("영어", "English"),
    ;
    private String koName;
    private String enName;
}
