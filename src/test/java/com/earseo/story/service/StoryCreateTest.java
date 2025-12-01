package com.earseo.story.service;

import com.earseo.story.common.exception.BaseException;
import com.earseo.story.dto.request.CreateRequest;
import com.earseo.story.dto.response.CreateResponse;
import com.earseo.story.entity.*;
import com.earseo.story.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("이야기 생성을 할 때 ")
class StoryCreateTest {

    private static final GeometryFactory GEOMETRY_FACTORY =
        new GeometryFactory(new PrecisionModel(), 4326);

    @InjectMocks
    private StoryService storyService;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private StoryAuthorRepository storyAuthorRepository;

    @Mock
    private StorySpotRepository storySpotRepository;

    @Mock
    private StoryImageRepository storyImageRepository;

    @Mock
    private StoryTitleRepository storyTitleRepository;

    @Mock
    private SpotTitleAggregateRepository spotTitleAggregateRepository;

    @Mock
    private S3Service s3Service;

    @Test
    @DisplayName("신규 작성자와 신규 스팟으로 이야기 생성 성공")
    void createStory_WithNewAuthorAndNewSpot_Success() {
        // Given
        Long memberId = 1L;
        LocalDateTime now = LocalDateTime.now();
        CreateRequest request = createRequest(1L, now);
        List<MultipartFile> images = new ArrayList<>();

        StoryAuthor savedAuthor = createStoryAuthor(1L, "이어동", now);
        StorySpot savedSpot = createStorySpot(1L, "wydm6djcm");
        StoryTitle storyTitle = createStoryTitle(1L, "골목길 맛집");
        Story savedStory = createStory(1L, savedAuthor, savedSpot, storyTitle, now);

        given(storyAuthorRepository.findById(1L)).willReturn(Optional.empty());
        given(storyAuthorRepository.save(any(StoryAuthor.class))).willReturn(savedAuthor);
        given(storySpotRepository.findByGeohash(anyString())).willReturn(Optional.empty());
        given(storySpotRepository.save(any(StorySpot.class))).willReturn(savedSpot);
        given(storyRepository.save(any(Story.class))).willReturn(savedStory);
        given(storyTitleRepository.save(any(StoryTitle.class))).willReturn(storyTitle);

        // When
        CreateResponse response = storyService.createStory(memberId, request, images);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.storyId()).isEqualTo(1L);
        assertThat(response.storySpotId()).isEqualTo(1L);
        assertThat(response.createdAt()).isEqualTo(now);

        verify(storyAuthorRepository, times(1)).findById(1L);
        verify(storyAuthorRepository, times(1)).save(any(StoryAuthor.class));
        verify(storySpotRepository, times(1)).findByGeohash(anyString());
        verify(storySpotRepository, times(1)).save(any(StorySpot.class));
        verify(storyRepository, times(1)).save(any(Story.class));
    }

    @Test
    @DisplayName("기존 작성자와 기존 스팟으로 이야기 생성 성공")
    void createStory_WithExistingAuthorAndSpot_Success() {
        // Given
        Long memberId = 1L;
        LocalDateTime oldTime = LocalDateTime.now().minusDays(1);
        LocalDateTime now = LocalDateTime.now();
        CreateRequest request = createRequest(1L, oldTime);
        List<MultipartFile> images = new ArrayList<>();

        StoryAuthor existingAuthor = createStoryAuthor(1L, "이어동", oldTime);
        StorySpot existingSpot = createStorySpot(1L, "wydm6djcm");
        StoryTitle storyTitle = createStoryTitle(1L, "골목길 맛집");
        Story savedStory = createStory(1L, existingAuthor, existingSpot, storyTitle, now);

        given(storyAuthorRepository.findById(1L)).willReturn(Optional.of(existingAuthor));
        given(storySpotRepository.findByGeohash(anyString())).willReturn(Optional.of(existingSpot));
        given(storyRepository.save(any(Story.class))).willReturn(savedStory);
        given(storyTitleRepository.save(any(StoryTitle.class))).willReturn(storyTitle);

        // When
        CreateResponse response = storyService.createStory(memberId, request, images);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.storyId()).isEqualTo(1L);
        assertThat(response.storySpotId()).isEqualTo(1L);

        verify(storyAuthorRepository, times(1)).findById(1L);
        verify(storyAuthorRepository, never()).save(any(StoryAuthor.class));
        verify(storySpotRepository, times(1)).findByGeohash(anyString());
        verify(storySpotRepository, never()).save(any(StorySpot.class));
        verify(storyRepository, times(1)).save(any(Story.class));
    }

    @Test
    @DisplayName("작성자 프로필 정보 업데이트 후 이야기 생성 성공")
    void createStory_WithAuthorProfileUpdate_Success() {
        // Given
        Long memberId = 1L;
        LocalDateTime oldTime = LocalDateTime.now().minusDays(2);
        LocalDateTime newTime = LocalDateTime.now();

        CreateRequest request = new CreateRequest(
            1L,
            "이어남",
            "https://example.com/new-profile.jpg",
            newTime,
            37.5665,
            126.9780,
            "골목길 맛집",
            "경복궁 근처 맛있는 카페! 강추..!",
            StoryConcept.EXPERIENCE,
            Locale.EN
        );
        List<MultipartFile> images = new ArrayList<>();

        StoryAuthor existingAuthor = createStoryAuthor(1L, "이어동", oldTime);
        StorySpot existingSpot = createStorySpot(1L, "wydm6djcm");
        StoryTitle storyTitle = createStoryTitle(1L, "골목길 맛집");
        Story savedStory = createStory(1L, existingAuthor, existingSpot, storyTitle, newTime);

        given(storyAuthorRepository.findById(1L)).willReturn(Optional.of(existingAuthor));
        given(storySpotRepository.findByGeohash(anyString())).willReturn(Optional.of(existingSpot));
        given(storyRepository.save(any(Story.class))).willReturn(savedStory);
        given(storyTitleRepository.save(any(StoryTitle.class))).willReturn(storyTitle);

        // When
        CreateResponse response = storyService.createStory(memberId, request, images);

        // Then
        assertThat(response).isNotNull();

        verify(storyAuthorRepository, times(1)).findById(1L);
        verify(storyAuthorRepository, never()).save(any(StoryAuthor.class));
        verify(storyRepository, times(1)).save(any(Story.class));

        assertThat(existingAuthor.getNickname()).isEqualTo("이어남");
        assertThat(existingAuthor.getProfileUrl()).isEqualTo("https://example.com/new-profile.jpg");
        assertThat(existingAuthor.getUpdatedAt()).isEqualTo(newTime);
    }

    @Test
    @DisplayName("이미지와 함께 이야기 생성 성공")
    void createStory_WithImages_Success() {
        // Given
        Long memberId = 1L;
        LocalDateTime now = LocalDateTime.now();
        CreateRequest request = createRequest(1L, now);

        List<MultipartFile> images = List.of(
            createMockMultipartFile("image1.jpg", "image/jpeg"),
            createMockMultipartFile("image2.jpg", "image/jpeg")
        );

        StoryAuthor savedAuthor = createStoryAuthor(1L, "이어동", now);
        StorySpot savedSpot = createStorySpot(1L, "wydm6djcm");
        StoryTitle storyTitle = createStoryTitle(1L, "골목길 맛집");
        Story savedStory = createStory(1L, savedAuthor, savedSpot, storyTitle, now);

        given(storyAuthorRepository.findById(1L)).willReturn(Optional.of(savedAuthor));
        given(storySpotRepository.findByGeohash(anyString())).willReturn(Optional.of(savedSpot));
        given(storyRepository.save(any(Story.class))).willReturn(savedStory);
        given(storyTitleRepository.save(any(StoryTitle.class))).willReturn(storyTitle);
        given(s3Service.uploadFile(any(MultipartFile.class), anyString()))
            .willReturn("https://cdn.example.com/image1.jpg")
            .willReturn("https://cdn.example.com/image2.jpg");

        // When
        CreateResponse response = storyService.createStory(memberId, request, images);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.storyId()).isEqualTo(1L);

        verify(s3Service, times(2)).uploadFile(any(MultipartFile.class), anyString());
        verify(storyImageRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("작성자 ID 불일치로 이야기 생성 실패")
    void createStory_WithMismatchedAuthorId_ThrowsException() {
        // Given
        Long memberId = 1L;
        Long differentAuthorId = 2L;

        CreateRequest request = createRequest(differentAuthorId, LocalDateTime.now());
        List<MultipartFile> images = new ArrayList<>();

        // When & Then
        assertThatThrownBy(() -> storyService.createStory(memberId, request, images))
            .isInstanceOf(BaseException.class)
            .extracting(exception -> ((BaseException) exception).getErrorCode().getStatus())
            .isEqualTo("STR002");

        verify(storyAuthorRepository, never()).findById(any());
        verify(storySpotRepository, never()).findByGeohash(any());
        verify(storyRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미지 업로드 실패 시 예외 발생")
    void createStory_ImageUploadFails_ThrowsException() {
        // Given
        Long memberId = 1L;
        LocalDateTime now = LocalDateTime.now();
        CreateRequest request = createRequest(1L, now);

        List<MultipartFile> images = List.of(
            createMockMultipartFile("image1.jpg", "image/jpeg")
        );

        StoryAuthor savedAuthor = createStoryAuthor(1L, "이어동", now);
        StorySpot savedSpot = createStorySpot(1L, "wydm6djcm");
        StoryTitle storyTitle = createStoryTitle(1L, "골목길 맛집");
        Story savedStory = createStory(1L, savedAuthor, savedSpot, storyTitle, now);

        given(storyAuthorRepository.findById(1L)).willReturn(Optional.of(savedAuthor));
        given(storySpotRepository.findByGeohash(anyString())).willReturn(Optional.of(savedSpot));
        given(storyRepository.save(any(Story.class))).willReturn(savedStory);
        given(storyTitleRepository.save(any(StoryTitle.class))).willReturn(storyTitle);
        given(s3Service.uploadFile(any(MultipartFile.class), anyString()))
            .willThrow(new RuntimeException("S3 업로드 실패"));

        // When & Then
        assertThatThrownBy(() -> storyService.createStory(memberId, request, images))
            .isInstanceOf(BaseException.class)
            .extracting(exception -> ((BaseException) exception).getErrorCode().getStatus())
            .isEqualTo("STR003");

        verify(s3Service, times(1)).uploadFile(any(MultipartFile.class), anyString());
        verify(storyImageRepository, never()).saveAll(anyList());
    }

    private CreateRequest createRequest(Long authorId, LocalDateTime updatedAt) {
        return new CreateRequest(
            authorId,
            "이어동",
            "https://example.com/profile.jpg",
            updatedAt,
            37.5665,
            126.9780,
            "골목길 맛집",
            "경복궁 근처 맛있는 카페! 강추..!",
            StoryConcept.TIP,
            Locale.KO
        );
    }

    private StoryTitle createStoryTitle(Long id, String title) {
        return new StoryTitle(id, title);
    }

    private StoryAuthor createStoryAuthor(Long id, String nickname, LocalDateTime updatedAt) {
        return StoryAuthor.builder()
            .id(id)
            .nickname(nickname)
            .profileUrl("https://example.com/profile.jpg")
            .updatedAt(updatedAt)
            .build();
    }

    private StorySpot createStorySpot(Long id, String geohash) {
        Point center = GEOMETRY_FACTORY.createPoint(new Coordinate(126.9780, 37.5665));
        center.setSRID(4326);

        StorySpot spot = StorySpot.builder()
            .geohash(geohash)
            .center(center)
            .build();
        ReflectionTestUtils.setField(spot, "id", id);
        return spot;
    }

    private Story createStory(Long id, StoryAuthor author, StorySpot spot, StoryTitle storyTitle, LocalDateTime createdAt) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(126.9780, 37.5665));
        point.setSRID(4326);

        Story story = Story.builder()
            .storyAuthor(author)
            .storySpot(spot)
            .point(point)
            .storyTitle(storyTitle)
            .content("경복궁 근처 맛있는 카페! 강추..!")
            .locale(Locale.KO)
            .storyConcept(StoryConcept.TIP)
            .build();
        ReflectionTestUtils.setField(story, "id", id);
        ReflectionTestUtils.setField(story, "createdAt", createdAt);
        return story;
    }

    private MockMultipartFile createMockMultipartFile(String filename, String contentType) {
        return new MockMultipartFile(
            "images",
            filename,
            contentType,
            "test image content".getBytes()
        );
    }
}
