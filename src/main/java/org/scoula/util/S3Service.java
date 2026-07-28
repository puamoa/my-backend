package org.scoula.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j       // 로그 출력용 (log.info, log.error 사용 가능)
@Service     // Spring이 이 클래스를 서비스 Bean으로 등록
@RequiredArgsConstructor // final 필드를 자동으로 생성자 주입
public class S3Service {

    // S3Config에서 등록한 S3Client Bean이 자동 주입됩니다
    private final S3Client s3Client;

    // application.yml(또는 .properties)에서 버킷 이름을 읽어옵니다
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region}")
    private String region;

    /**
     * MultipartFile을 S3에 업로드합니다.
     *
     * @param file      업로드할 파일 (컨트롤러에서 받은 MultipartFile)
     * @param directory S3 내 저장 경로 (예: "public/board", "private/docs")
     * @return 업로드된 객체의 S3 key (예: "public/board/uuid.jpg")
     */
    public String upload(MultipartFile file, String directory) {
        // 원본 파일명에서 확장자 추출 (예: .jpg, .png)
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);

        // UUID로 고유한 파일명 생성 → 파일명 충돌 방지 + 보안
        // 결과 예: "public/board/550e8400-e29b-41d4-a716-446655440000.jpg"
        String key = directory + "/" + UUID.randomUUID() + extension;

        try {
            // S3에 보낼 요청 객체 생성 (어디에, 어떤 타입으로, 얼마나 큰 파일을 저장할지)
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)                    // 저장할 버킷 이름
                    .key(key)                          // 버킷 내 경로 (파일의 주소)
                    .contentType(file.getContentType()) // MIME 타입 (image/jpeg 등)
                    .contentLength(file.getSize())      // 파일 크기 (바이트)
                    .build();

            // 실제 S3로 파일 데이터를 전송합니다
            // fromInputStream: 파일을 스트림으로 읽어서 S3로 보냄
            s3Client.putObject(request,
                    RequestBody.fromInputStream(
                            file.getInputStream(), file.getSize()));

            log.info("파일 업로드 성공: {}", key);
            return key; // 저장된 S3 key를 반환 (DB에 저장할 값)

        } catch (IOException e) {
            throw new RuntimeException(
                    "파일 업로드 실패: " + originalFilename, e);
        }
    }

    /**
     * 특정 경로(prefix) 아래의 파일 목록을 조회합니다.
     * 예: prefix="public/board/" → 게시판에 업로드된 모든 파일의 key 반환
     */
    public List<String> listFiles(String prefix) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix) // 이 경로로 시작하는 객체만 조회
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        // 응답에서 각 객체의 key만 추출하여 리스트로 반환
        return response.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    /**
     * S3에서 파일을 다운로드합니다.
     * 반환값은 InputStream이므로 컨트롤러에서 응답으로 내려보낼 수 있습니다.
     */
    public ResponseInputStream<GetObjectResponse> download(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        return s3Client.getObject(request);
    }

    /**
     * S3에서 파일을 삭제합니다.
     * 게시글 삭제 시 첨부파일도 함께 삭제할 때 사용합니다.
     */
    public void delete(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3Client.deleteObject(request);
        log.info("파일 삭제 성공: {}", key);
    }

    /**
     * S3 key로부터 브라우저에서 접근 가능한 전체 URL을 생성합니다.
     * public/ 경로의 파일은 이 URL로 바로 접근할 수 있습니다.
     * 예: "https://my-bucket.s3.ap-northeast-2.amazonaws.com/public/board/uuid.jpg"
     */
    public String getFileUrl(String key) {
        return String.format(
                "https://%s.s3.%s.amazonaws.com/%s",
                bucket, region, key);
    }

    /**
     * 파일명에서 확장자를 추출하는 유틸 메서드
     * "photo.jpg" → ".jpg", "document" → ""
     */
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
