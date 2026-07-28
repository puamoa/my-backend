package org.scoula.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
@Profile("aws")  // EC2 배포 시에만 활성화
@RequiredArgsConstructor
public class AwsDataSourceConfig {

    private final SecretsManagerService secretsManager;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");       // MySQL 기본 드라이버
        config.setJdbcUrl(secretsManager.getJdbcUrl());              // Secrets Manager에서 조합된 URL
        config.setUsername(secretsManager.getDbUsername());           // Secrets Manager에서 조회
        config.setPassword(secretsManager.getDbPassword());          // Secrets Manager에서 조회
        return new HikariDataSource(config);
    }
//    private final ParameterStoreService parameterStoreService;
//
//    @Bean
//    public DataSource dataSource() {
//        HikariConfig config = new HikariConfig();
//        config.setDriverClassName(parameterStoreService.getDbDriver());
//        config.setJdbcUrl(parameterStoreService.getDbUrl());
//        config.setUsername(parameterStoreService.getDbUsername());
//        config.setPassword(parameterStoreService.getDbPassword());
//        return new HikariDataSource(config);
//    }
}