package com.earseo.story.event.consumer;


import com.earseo.story.common.KafkaEvent;
import com.earseo.story.event.dto.TestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestConsumer {
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "story-test",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void listenEvent(
        @Payload KafkaEvent event,
        Acknowledgment ack
    ) {
        try {
            TestDto eventObject = objectMapper.convertValue(event.getData(), TestDto.class);
            log.info("Received event {}", eventObject);
            ack.acknowledge();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
