package org.scoula.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

@Slf4j
@Component
@Profile("aws")  // "aws" 프로필일 때만 Bean 등록
public class SecretsManagerService {

    private final SecretsManagerClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();  // JSON 파싱용

    private String dbHost;
    private String dbPort;
    private String dbName;
    private String dbUsername;
    private String dbPassword;

    public SecretsManagerService() {
        // 서울 리전의 Secrets Manager 클라이언트 생성
        this.client = SecretsManagerClient.builder()
                .region(Region.AP_NORTHEAST_2)
                .build();
    }

    /**
     * 애플리케이션 시작 시 Secrets Manager에서 DB 자격 증명을 로드합니다.
     * Parameter Store와 달리 JSON 형태로 여러 값이 하나의 비밀에 저장됩니다.
     */
    @PostConstruct
    public void loadSecrets() {
        try {
            // Secrets Manager에서 비밀 값 조회
            GetSecretValueResponse response = client.getSecretValue(
                    GetSecretValueRequest.builder()
                            .secretId("starter/prod/db-credentials")  // 태스크 2에서 생성한 비밀 이름
                            .build()
            );

            // JSON 문자열을 파싱하여 각 필드 추출
            JsonNode secret = objectMapper.readTree(response.secretString());
            this.dbHost = secret.get("host").asText();
            this.dbPort = secret.get("port").asText();
            this.dbName = secret.get("dbname").asText();
            this.dbUsername = secret.get("username").asText();
            this.dbPassword = secret.get("password").asText();

            log.info("Secrets Manager에서 DB 자격 증명 로드 완료");
            log.info("  host: {}", dbHost);
            log.info("  port: {}", dbPort);
            log.info("  dbname: {}", dbName);
            log.info("  username: {}", dbUsername);
            log.info("  password: {}", "****");  // 비밀번호는 마스킹
        } catch (Exception e) {
            log.error("Secrets Manager 조회 실패", e);
            throw new RuntimeException("Failed to load secrets", e);
        }
    }

    /** host, port, dbname을 조합하여 JDBC URL 생성 */
    public String getJdbcUrl() {
        return String.format("jdbc:mysql://%s:%s/%s", dbHost, dbPort, dbName);
    }

    public String getDbUsername() { return dbUsername; }
    public String getDbPassword() { return dbPassword; }
}