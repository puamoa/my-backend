package org.scoula.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration // Spring이 이 클래스를 설정 파일로 인식하여 Bean을 등록합니다
public class S3Config {

    @Value("${cloud.aws.region}") // application.yml의 cloud.aws.region 값을 주입
    private String region;

    @Bean // S3 업로드/다운로드/삭제 등 기본 작업에 사용하는 클라이언트
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region)) // 서울 리전 지정
                .build(); // credentialsProvider 미지정 → 자동 탐색 (Credential Chain)
    }

    @Bean // Presigned URL 생성 전용 클라이언트
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }
}