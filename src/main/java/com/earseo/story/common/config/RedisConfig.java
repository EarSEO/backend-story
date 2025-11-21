package com.earseo.story.common.config;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.MicrometerTracing;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.sentinel.master}")
    private String sentinelMaster;
    @Value("${spring.data.redis.sentinel.nodes}")
    private String sentinelNodes;
    @Value("${spring.data.redis.database}")
    private int database;
    @Value("${spring.data.redis.password}")
    private String password;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Sentinel 구성
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
            .master(sentinelMaster);

        for (String node : sentinelNodes.split(",")) {
            String[] addr = node.split(":");
            sentinelConfig.sentinel(addr[0], Integer.parseInt(addr[1]));
        }

        sentinelConfig.setPassword(password);
        sentinelConfig.setDatabase(database);

        // 읽기 분산 전략 설정
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            // 가능하면 Replica에서 읽고, 안 되면 Master에서 읽음
            .readFrom(ReadFrom.REPLICA_PREFERRED)
            .build();

        return new LettuceConnectionFactory(sentinelConfig, clientConfig);
    }

    @Bean(name = "stringTemplate")
    @Primary
    public RedisTemplate<String, String> stringTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return redisTemplate;
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return redisTemplate;
    }

    @Bean
    public ClientResources clientResources(ObservationRegistry observationRegistry) {
        return ClientResources.builder()
            .tracing(new MicrometerTracing(observationRegistry, "redis-cache"))
            .build();
    }
}
