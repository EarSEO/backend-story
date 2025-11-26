package com.earseo.story.repository;

import com.earseo.story.entity.Locale;
import com.earseo.story.entity.Story;
import com.earseo.story.entity.StoryConcept;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class StoryJdbcRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<Long> findHotStorySpot(int hotSpotThreshold) {
        String sql = """
            SELECT story_spot_id
            FROM story
            GROUP BY story_spot_id
            HAVING COUNT(*) >= :hotSpotThreshold
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("hotSpotThreshold", hotSpotThreshold);

        return namedParameterJdbcTemplate.queryForList(sql, params, Long.class);
    }


    public Map<Long, Map<StoryConcept, Set<Story>>> findAllHotStorySummarizeTargetStory(List<Long> hotSpotIds, int storySummaryLimit) {
        if (hotSpotIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String sql = """
            WITH ranked_stories AS (
                SELECT 
                    s.*,
                    ROW_NUMBER() OVER (
                        PARTITION BY s.story_spot_id, s.story_concept 
                        ORDER BY s.like_count DESC, s.created_at DESC
                    ) as rn
                FROM story s
                WHERE s.story_spot_id IN (:hotSpotIds)
            )
            SELECT
                story_id,
                story_spot_id,
                story_concept,
                locale,
                content,
                created_at
            FROM ranked_stories
            WHERE rn <= :storySummaryLimit
            ORDER BY story_spot_id, story_concept, rn
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("hotSpotIds", hotSpotIds);
        params.put("storySummaryLimit", storySummaryLimit);

        Map<Long, Map<StoryConcept, Set<Story>>> result = new HashMap<>();

        namedParameterJdbcTemplate.query(sql, params, rs -> {
            Long storySpotId = rs.getLong("story_spot_id");
            StoryConcept storyConcept = StoryConcept.valueOf(rs.getString("story_concept"));
            Story story = mapRowToStory(rs);

            result
                .computeIfAbsent(storySpotId, k -> new HashMap<>())
                .computeIfAbsent(storyConcept, k -> new HashSet<>())
                .add(story);
        });

        return result;
    }

    private Story mapRowToStory(ResultSet rs) throws SQLException {
        return Story.builder()
            .id(rs.getLong("story_id"))
            .content(rs.getString("content"))
            .storyConcept(StoryConcept.valueOf(rs.getString("story_concept")))
            .locale(Locale.valueOf(rs.getString("locale")))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();
    }
}
