package org.scoula.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.log4j.Log4j2;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@PropertySource(value = {"classpath:/application.properties"}, encoding = "UTF-8")
@MapperScan(basePackages = {"org.scoula.board.mapper", "org.scoula.member.mapper", "org.scoula.travel.mapper"})
@ComponentScan(basePackages = {"org.scoula.board.service", "org.scoula.member.service", "org.scoula.travel.service", "org.scoula.util", "org.scoula.config"})
@org.springframework.context.annotation.Import(S3Config.class)
@Log4j2
@EnableTransactionManagement
public class RootConfig {
//    @Value("${jdbc.driver}")
//    String driver;
//
//    @Value("${jdbc.url}")
//    String url;
//
//    @Value("${jdbc.username}")
//    String username;
//
//    @Value("${jdbc.password}")
//    String password;

    @Autowired
    ApplicationContext applicationContext;

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        //                                     ↑ 파라미터로 주입받음 (외부 Config에서 생성된 Bean)
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setConfigLocation(applicationContext.getResource("classpath:/mybatis-config.xml"));
        factory.setDataSource(dataSource);
        return (SqlSessionFactory) factory.getObject();
    }

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
//    @Bean
//    public DataSource dataSource() {
//        HikariConfig config = new HikariConfig();
//
//        config.setDriverClassName(driver);
//        config.setJdbcUrl(url);
//        config.setUsername(username);
//        config.setPassword(password);
//
//        HikariDataSource dataSource = new HikariDataSource(config);
//        return dataSource;

//    }
//    @Bean
//    public SqlSessionFactory sqlSessionFactory() throws Exception {
//        SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
//        sqlSessionFactory.setConfigLocation(applicationContext.getResource("classpath:/mybatis-config.xml"));
//        sqlSessionFactory.setDataSource(dataSource());
//        return (SqlSessionFactory) sqlSessionFactory.getObject();
//    }
//
//    @Bean
//    public DataSourceTransactionManager transactionManager() {
//        DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource());
//        return manager;

//    }
}
