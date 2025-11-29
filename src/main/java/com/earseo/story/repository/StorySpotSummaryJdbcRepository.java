package com.earseo.story.repository;

import com.earseo.story.entity.Locale;
import com.earseo.story.entity.Story;
import com.earseo.story.entity.StoryConcept;
import com.earseo.story.service.OpenAiService.SpotSummaryResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class StorySpotSummaryJdbcRepository {

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public Map<Long, Map<StoryConcept, Set<Long>>> findAllLastSummarizedStoryIdSet(List<Long> hotSpotIds) {
        if (hotSpotIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String sql = """
            SELECT DISTINCT ON (story_spot_id, story_concept)
                story_spot_id,
                story_concept,
                summarized_story_id_set
            FROM story_spot_summary
            WHERE story_spot_id IN (:hotSpotIds)
            ORDER BY story_spot_id,story_concept,updated_at DESC
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("hotSpotIds", hotSpotIds);

        Map<Long, Map<StoryConcept, Set<Long>>> result = new HashMap<>();

        namedParameterJdbcTemplate.query(sql, params, rs -> {
            Long storySpotId = rs.getLong("story_spot_id");
            StoryConcept storyConcept = StoryConcept.valueOf(rs.getString("story_concept"));
            String storyIdSet = rs.getString("summarized_story_id_set");
            Set<Long> set = deserailizeSummarizedStoryIdSet(storyIdSet);

            result
                .computeIfAbsent(storySpotId, k -> new HashMap<>())
                .put(storyConcept, set);
        });

        return result;
    }

    @Transactional
    public long batchUpsert(
        List<SpotSummarySaveRequest> requests
    ) {
        if (requests.isEmpty()) {
            return 0;
        }

        String sql = """
            INSERT INTO story_spot_summary 
                (story_spot_id, story_concept, locale, title, summary, summarized_story_id_set, updated_at)
            VALUES (?, ?, ?, ?, ?, ?::jsonb, CURRENT_TIMESTAMP)
            ON CONFLICT ON CONSTRAINT spot_concept_locale_constraint
            DO UPDATE SET
               title = EXCLUDED.title,
               summary = EXCLUDED.summary,
               summarized_story_id_set = EXCLUDED.summarized_story_id_set,
               updated_at = CURRENT_TIMESTAMP 
            """;

        // 배치 크기를 나눠서 처리 (메모리 효율)
        int batchSize = 1000;
        long insertedSummaryCnt = 0;
        for (int i = 0; i < requests.size(); i += batchSize) {
            int end = Math.min(i + batchSize, requests.size());
            List<SpotSummarySaveRequest> batch = requests.subList(i, end);

            jdbcTemplate.batchUpdate(
                sql,
                batch,
                batch.size(),
                (PreparedStatement ps, SpotSummarySaveRequest request) -> {
                    ps.setLong(1, request.storySpotId());
                    ps.setString(2, request.storyConcept().name());
                    ps.setString(3, request.locale().name());
                    ps.setString(4, request.title());
                    ps.setString(5, request.summary());
                    ps.setString(6, serializeSummarizedStoryIdSet(request.stories()));
                }
            );

            insertedSummaryCnt += batch.size();
        }
        return insertedSummaryCnt;
    }

    private Set<Long> deserailizeSummarizedStoryIdSet(String storyIdSet) {
        try {
            return objectMapper.readValue(storyIdSet, new TypeReference<Set<Long>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize story IDs", e);
        }
    }

    public String serializeSummarizedStoryIdSet(Set<Story> stories) {
        Set<Long> storyIds = stories.stream()
            .map(Story::getId)
            .collect(Collectors.toSet());

        try {
            return objectMapper.writeValueAsString(storyIds);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize story IDs", e);
        }
    }

    public record SpotSummarySaveRequest(
        Long storySpotId,
        StoryConcept storyConcept,
        Locale locale,
        String title,
        String summary,
        Set<Story> stories
    ) {
        public static SpotSummarySaveRequest toDto(
            SpotSummaryResult spotSummaryResult,
            Long storySpotId,
            StoryConcept storyConcept,
            Locale locale,
            Set<Story> stories
        ) {
            return new SpotSummarySaveRequest(
                storySpotId,
                storyConcept,
                locale,
                spotSummaryResult.title(),
                spotSummaryResult.summary(),
                stories
            );
        }
    }
}
