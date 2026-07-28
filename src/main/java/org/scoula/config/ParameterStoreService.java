package org.scoula.config;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@Profile("aws-ssm")  // "aws" 프로필일 때만 Bean 등록
public class ParameterStoreService {

    private final SsmClient ssmClient;
    private final Map<String, String> parameters = new HashMap<>();

    public ParameterStoreService() {
        this.ssmClient = SsmClient.builder()
                .region(Region.AP_NORTHEAST_2)
                .build();
    }

    /**
     * 애플리케이션 시작 시 Parameter Store에서 설정값을 로드합니다.
     */
    @PostConstruct
    public void loadParameters() {
        String path = "/starter/prod";
        log.info("Parameter Store에서 설정 로드 시작: {}", path);

        GetParametersByPathRequest request = GetParametersByPathRequest.builder()
                .path(path)
                .recursive(true)
                .withDecryption(true)
                .build();

        GetParametersByPathResponse response = ssmClient.getParametersByPath(request);

        for (Parameter param : response.parameters()) {
            parameters.put(param.name(), param.value());
            log.info("파라미터 로드: {} (type: {})", param.name(), param.type());
        }

        log.info("총 {}개 파라미터 로드 완료", parameters.size());
    }

    /**
     * 단일 파라미터를 조회합니다.
     */
    public String getParameter(String name) {
        // 캐시된 값이 있으면 반환
        if (parameters.containsKey(name)) {
            return parameters.get(name);
        }

        // 없으면 직접 조회
        GetParameterRequest request = GetParameterRequest.builder()
                .name(name)
                .withDecryption(true)
                .build();

        String value = ssmClient.getParameter(request).parameter().value();
        parameters.put(name, value);
        return value;
    }

    public String getDbUrl() {
        return getParameter("/starter/prod/db/url");
    }

    public String getDbDriver() {
        return getParameter("/starter/prod/db/driver");
    }

    public String getDbUsername() {
        return getParameter("/starter/prod/db/username");
    }

    public String getDbPassword() {
        return getParameter("/starter/prod/db/password");
    }

    public String getS3Bucket() {
        return getParameter("/starter/prod/s3/bucket");
    }

}