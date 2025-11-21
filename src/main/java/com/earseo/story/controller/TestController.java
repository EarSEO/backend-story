package com.earseo.story.controller;

import com.earseo.story.event.producer.TestProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final RedisTemplate<String, String> redisTemplate;
    private final TestProducer testProducer;

    @GetMapping("/redis/set")
    public String hello() {
        LocalDateTime now = LocalDateTime.now();
        redisTemplate.opsForValue().set("hello", now.toString());
        return now.toString();
    }

    @GetMapping("/redis/get")
    public String get() {
        return redisTemplate.opsForValue().get("hello");
    }

    @GetMapping("/kafka")
    public String kafka() {
        testProducer.test();
        return String.format("success : %s", LocalDateTime.now());
    }
}
