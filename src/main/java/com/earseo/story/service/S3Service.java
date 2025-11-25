package com.earseo.story.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudFrontDomain;

    private final AmazonS3 amazonS3;

    /**
     * S3에 파일 업로드하고 CloudFront URL 반환
     */
    public String uploadFile(MultipartFile file, String directory) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String savedFilename = UUID.randomUUID() + extension;
        String fileKey = directory + "/" + savedFilename;
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            amazonS3.putObject(new PutObjectRequest(bucket, fileKey, file.getInputStream(), metadata));
            return getCloudFrontUrl(fileKey);
        } catch (IOException e) {
            log.error("S3 파일 업로드 실패: {}", e.getMessage());
            throw new RuntimeException("파일 업로드에 실패했습니다.", e);
        }
    }

    /**
     * S3에서 파일 삭제 (CloudFront URL을 S3 key로 변환)
     */
    public void deleteFile(String fileUrl) {
        try {
            String fileKey = extractFileKeyFromUrl(fileUrl);
            amazonS3.deleteObject(bucket, fileKey);
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패 {} : {}", fileUrl, e.getMessage());
        }
    }

    /**
     * CloudFront URL 생성
     */
    private String getCloudFrontUrl(String fileKey) {
        return String.format("https://%s/%s", cloudFrontDomain, fileKey);
    }

    /**
     * CloudFront URL 또는 S3 URL에서 파일 키 추출
     */
    private String extractFileKeyFromUrl(String fileUrl) {
        // CloudFront URL인 경우
        if (fileUrl.contains(cloudFrontDomain)) {
            return fileUrl.substring(fileUrl.indexOf(cloudFrontDomain) + cloudFrontDomain.length() + 1);
        }
        // S3 URL인 경우
        if (fileUrl.contains(bucket)) {
            return fileUrl.substring(fileUrl.indexOf(bucket) + bucket.length() + 1);
        }
        throw new IllegalArgumentException("유효하지 않은 파일 URL입니다: " + fileUrl);
    }
}
