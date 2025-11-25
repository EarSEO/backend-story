package com.earseo.story.service;

import com.earseo.story.entity.Locale;
import com.earseo.story.entity.Story;
import com.earseo.story.entity.StoryConcept;
import com.earseo.story.service.OpenAiService.SpotSummaryResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@SpringBootTest
@ActiveProfiles("local")
@DisplayName("OpenAiService 테스트용 ")
class OpenAiServiceIntegrationTest {

    @Autowired
    private OpenAiService openAiService;

    @Test
    @DisplayName("OpenAI API 영어 요약 생성 (수동 실행용)")
    void generateSummaryWithAI_RealAPI_GeneratesSummary() {
        // given
        Set<Story> stories = Set.of(
            createStory(1L, "500년 역사를 자랑하는 궁궐입니다."),
            createStory(2L, "Traditional Korean architecture is stunning."),
            createStory(3L, "A must-visit cultural heritage site.")
        );

        // when
        SpotSummaryResult result = openAiService.generateSummaryWithAI(
            stories,
            StoryConcept.HISTORY,
            Locale.EN
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.title()).isNotBlank();
        assertThat(result.summary()).isNotBlank();

        System.out.println("=====OpenAI 응답=====");
        System.out.println("제목: " + result.title());
        System.out.println("요약: " + result.summary());
    }

    @Test
    @DisplayName("OpenAI API 한국어 요약 생성 (수동 실행용)")
    void generateSummaryWithAI_RealAPI_Korean_GeneratesSummary() {
        // given
        Set<Story> stories = Set.of(
            createStory(1L, "떡볶이집이 너무 맛있어요. 게다가 떡볶이집에 떡이 무한리필이에요... 사장님 진짜 대박나세요... ㅠ"),
            createStory(2L, "Makgeolli is really good and the snacks are delicious. It's a place with a lot of foreigners and tourists."),
            createStory(3L, "여기에서 체험하는 술 문화인 주도가 굉장히 재밌어요. 한국인이지만 몰랐던게 많네,,")
        );

        // when
        SpotSummaryResult result = openAiService.generateSummaryWithAI(
            stories,
            StoryConcept.HISTORY,
            Locale.KO
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.title()).isNotBlank();
        assertThat(result.summary()).isNotBlank();

        System.out.println("=====OpenAI 응답=====");
        System.out.println("제목: " + result.title());
        System.out.println("요약: " + result.summary());
    }

    private Story createStory(Long id, String content) {
        return Story.builder()
            .id(id)
            .content(content)
            .storyConcept(StoryConcept.EXPERIENCE)
            .locale(Locale.EN)
            .createdAt(LocalDateTime.now())
            .build();
    }
}
