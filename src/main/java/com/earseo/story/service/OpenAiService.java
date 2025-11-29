package com.earseo.story.service;

import com.earseo.story.entity.Locale;
import com.earseo.story.entity.Story;
import com.earseo.story.entity.StoryConcept;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpenAiService {
    private final ChatClient chatClient;

    public SpotSummaryResult generateSummaryWithAI(Set<Story> stories, StoryConcept concept, Locale locale) {
        String combinedContent = stories.stream()
            .map(Story::getContent)
            .collect(Collectors.joining("\n\n---\n\n"));

        String languageInstruction = String.format("You must respond in %s language.", locale.getEnName());

        SpotSummaryResult spotSummaryResult = chatClient.prompt()
            .system(s -> s.text("""
                You are an expert content analyst specializing in travel and location-based user-generated content.
                
                Your responsibilities:
                - Analyze multiple user posts and extract key themes
                - Create concise, accurate summaries that capture the essence of the content
                - Maintain objectivity and avoid personal opinions
                - Ensure summaries are informative and easy to understand
                
                Output requirements:
                - Title: 5-10 words, clear and descriptive
                - Summary: 2-3 complete sentences
                - Tone: Professional and neutral
                - Language: Follow the user's language instruction strictly
                """))
            .user(u -> u.text("""
                    Analyze the following user posts from the {concept} category:
                    
                    === USER POSTS ===
                    {content}
                    === END OF POSTS ===
                    
                    Create a summary with:
                    1. A concise title capturing the main theme
                    2. A 2-3 sentence summary of key points
                    
                    {language}
                    """)
                .param("concept", concept.getEnName())
                .param("content", combinedContent)
                .param("language", languageInstruction))
            .call()
            .entity(SpotSummaryResult.class);

        return spotSummaryResult;
    }

    public record SpotSummaryResult(
        String title,
        String summary
    ) {
    }
}
