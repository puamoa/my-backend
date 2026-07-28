package org.scoula.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresignedUrlService {

    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * Presigned Upload URL을 생성합니다.
     * 클라이언트가 이 URL에 PUT 요청으로 파일을 직접 S3에 업로드할 수 있습니다.
     *
     * @param directory   S3 저장 경로 (예: "test", "public/images", "private/docs")
     * @param filename    원본 파일명 (예: "hello.txt", "photo.png")
     * @param contentType MIME 타입 (예: "text/plain", "image/png")
     * @return 서명된 업로드 URL (10분간 유효)
     */
    public String generateUploadUrl(String directory, String filename, String contentType) {
        // UUID + 원본 파일명으로 고유한 key 생성
        String key = directory + "/" + UUID.randomUUID() + "_" + filename;

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))  // 10분간 유효
                .putObjectRequest(putRequest)
                .build();

        String url = s3Presigner.presignPutObject(presignRequest).url().toString();
        log.info("Presigned Upload URL 생성: key={}", key);
        return url;
    }

    /**
     * Presigned Download URL을 생성합니다.
     * 이 URL을 브라우저에 붙여넣거나 fetch로 호출하면 파일을 다운로드할 수 있습니다.
     *
     * @param key S3 객체 key (예: "test/uuid_hello.txt")
     * @return 서명된 다운로드 URL (30분간 유효)
     */
    public String generateDownloadUrl(String key) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(30))  // 30분간 유효
                .getObjectRequest(getRequest)
                .build();

        String url = s3Presigner.presignGetObject(presignRequest).url().toString();
        log.info("Presigned Download URL 생성: key={}", key);
        return url;
    }
}
