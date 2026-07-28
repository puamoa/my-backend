package org.scoula;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;

public class SsmConnectionTest {
    public static void main(String[] args) {
        SsmClient client = SsmClient.builder()
                .region(Region.AP_NORTHEAST_2)
                .build();

        GetParametersByPathResponse response = client.getParametersByPath(
                GetParametersByPathRequest.builder()
                        .path("/starter/prod")
                        .recursive(true)
                        .withDecryption(true)
                        .build()
        );

        response.parameters().forEach(p ->
                System.out.println(p.name() + " = " + p.value())
        );

        client.close();
        System.out.println("\n✅ SSM Parameter Store 연동 테스트 성공!");
    }
}