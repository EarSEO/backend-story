package com.earseo.story.event.producer;

import com.earseo.story.common.KafkaEvent;
import com.earseo.story.event.dto.TestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestProducer {
    @Value("${spring.application.name}")
    private String applicationName;
    private final String testTopic = "story-test";

    private final KafkaTemplate kafkaTemplate;

    public void test() {
        KafkaEvent kafkaEvent = KafkaEvent.builder()
            .eventId(applicationName + UUID.randomUUID().toString())
            .eventTime(LocalDateTime.now().toString())
            .eventType("testType")
            .data(new TestDto("hello", 123, LocalDateTime.now()))
            .build();
        log.info("Sending data : {}", kafkaEvent);
        kafkaTemplate.send(testTopic, kafkaEvent);
    }
}
