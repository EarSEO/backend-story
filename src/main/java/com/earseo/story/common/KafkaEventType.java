package com.earseo.story.common;

public class KafkaEventType {
    public static final String STORY_UPDATED = "story.updated";
    public static final String STORY_LIKED = "story.liked";
    public static final String STORY_REPORTED = "story.reported";

    private KafkaEventType() {
    }
}