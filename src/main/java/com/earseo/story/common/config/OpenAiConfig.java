package com.earseo.story.common.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {
    @Value("${openai.credential.access-key}")
    private String openAiApiKey;
    @Value("${openai.model:gpt-4o-mini}")
    private String openAiModel;
    @Value("${openai.temperature:0.1}")
    private Double openAiTemperature;
    @Value("${openai.top-p:0.9}")
    private Double topP;
    @Value("${openai.max-token:}")
    private Integer openAiMaxToken;

    @Bean
    public ChatClient storySummaryChatClient() {
        return ChatClient.builder(OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                    .apiKey(openAiApiKey)
                    .build())
                .defaultOptions(OpenAiChatOptions.builder()
                    .model(openAiModel)
                    .temperature(openAiTemperature)
                    .topP(0.9)
                    .maxTokens(openAiMaxToken)
                    .build())
                .build())
            .build();
    }
}
