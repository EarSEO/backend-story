package com.earseo.story.entity;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum StoryConcept {
    TIP("꿀팁","Tip"),
    EXPERIENCE("경험담","Experience"),
    CULTURE("문화","Culture"),
    HISTORY("역사","History"),
    ETC("역사","Etc"),
    ;
    private final String koName;
    private final String enName;
}
