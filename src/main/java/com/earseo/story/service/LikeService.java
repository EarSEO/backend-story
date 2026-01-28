package com.earseo.story.service;

import com.earseo.story.common.exception.BaseException;
import com.earseo.story.entity.Story;
import com.earseo.story.entity.StoryLike;
import com.earseo.story.repository.StoryLikeRepository;
import com.earseo.story.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static com.earseo.story.common.exception.StoryError.STORY_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {

    private static final String LIKE_KEY_PREFIX = "story:like:";
    private static final String LIKE_COUNT_KEY_PREFIX = "story:like:count:";
    private static final long CACHE_TTL_HOURS = 24;

    private final StoryRepository storyRepository;
    private final StoryLikeRepository storyLikeRepository;
//    private final RedisTemplate<String, String> stringTemplate;
    private final StringRedisTemplate stringTemplate;

    @Transactional
    public boolean toggleLike(Long storyId, Long memberId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new BaseException(STORY_NOT_FOUND));

        String likeKey = LIKE_KEY_PREFIX + storyId + ":" + memberId;
        String countKey = LIKE_COUNT_KEY_PREFIX + storyId;

        // Redis에 countKey가 없으면 DB 값으로 초기화
        if (Boolean.FALSE.equals(stringTemplate.hasKey(countKey))) {
            stringTemplate.opsForValue().set(
                    countKey,
                    String.valueOf(story.getLikeCount()),
                    CACHE_TTL_HOURS,
                    TimeUnit.HOURS
            );
        }

        // 좋아요 존재 여부 확인
        boolean isLiked = storyLikeRepository.existsByStoryIdAndMemberId(storyId, memberId);

        if (isLiked) {
            // 좋아요 취소
            StoryLike storyLike = storyLikeRepository.findByStoryIdAndMemberId(storyId, memberId)
                    .orElseThrow(() -> new BaseException(STORY_NOT_FOUND));
            storyLikeRepository.delete(storyLike);
            story.decrementLikeCount();

            // Redis 캐시 삭제
            stringTemplate.delete(likeKey);
            stringTemplate.opsForValue().decrement(countKey);

            return false;
        } else {
            // 좋아요 추가
            StoryLike storyLike = StoryLike.builder()
                    .story(story)
                    .memberId(memberId)
                    .build();
            storyLikeRepository.save(storyLike);
            story.incrementLikeCount();

            // Redis 캐시 저장
            stringTemplate.opsForValue().set(likeKey, "1", CACHE_TTL_HOURS, TimeUnit.HOURS);
            stringTemplate.opsForValue().increment(countKey);
            stringTemplate.expire(countKey, CACHE_TTL_HOURS, TimeUnit.HOURS);

            return true;
        }
    }

    public Long getLikeCount(Long storyId) {
        String countKey = LIKE_COUNT_KEY_PREFIX + storyId;

        // Redis에서 먼저 조회
        Object cachedCount = stringTemplate.opsForValue().get(countKey);
        if (cachedCount != null) {
            Long count;
            if (cachedCount instanceof String) {
                count = Long.parseLong((String) cachedCount);
            } else if (cachedCount instanceof Number) {
                count = ((Number) cachedCount).longValue();
            } else {
                count = null;
            }

            if (count != null) {
                return count;
            }
        }

        // Redis에 없으면 DB 조회
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new BaseException(STORY_NOT_FOUND));

        Long likeCount = story.getLikeCount();

        // Redis에 캐싱
        stringTemplate.opsForValue().set(countKey, String.valueOf(likeCount), CACHE_TTL_HOURS, TimeUnit.HOURS);

        return likeCount;
    }

    public boolean isLiked(Long storyId, Long memberId) {
        String likeKey = LIKE_KEY_PREFIX + storyId + ":" + memberId;

        // Redis에서 먼저 조회
        Boolean exists = stringTemplate.hasKey(likeKey);
        if (Boolean.TRUE.equals(exists)) {
            return true;
        }

        // Redis에 없으면 DB 조회
        boolean isLiked = storyLikeRepository.existsByStoryIdAndMemberId(storyId, memberId);

        // 좋아요 되어있으면 Redis에 캐싱
        if (isLiked) {
            stringTemplate.opsForValue().set(likeKey, "1", CACHE_TTL_HOURS, TimeUnit.HOURS);
        }

        return isLiked;
    }
}