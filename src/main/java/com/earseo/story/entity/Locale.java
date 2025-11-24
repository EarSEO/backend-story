package com.earseo.story.entity;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Locale {
    KO("한국어", "Korean"),
    EN("영어", "English"),
    ;
    private String koName;
    private String enName;
}
