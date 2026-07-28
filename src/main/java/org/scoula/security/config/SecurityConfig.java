package org.scoula.security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.mybatis.spring.annotation.MapperScan;
import org.scoula.security.filter.AuthenticationErrorFilter;
import org.scoula.security.filter.JwtAuthenticationFilter;
import org.scoula.security.filter.JwtUsernamePasswordAuthenticationFilter;
import org.scoula.security.handler.CustomAccessDeniedHandler;
import org.scoula.security.handler.CustomAuthenticationEntryPoint;
import org.scoula.security.handler.LoginFailureHandler;
import org.scoula.security.handler.LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@MapperScan(basePackages = {"org.scoula.security.account.mapper"})
@ComponentScan(basePackages = {"org.scoula.security"})
@RequiredArgsConstructor
@Log4j2
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationErrorFilter authenticationErrorFilter;

    @Autowired
    private LoginSuccessHandler loginSuccessHandler;

    @Autowired
    private LoginFailureHandler loginFailureHandler;

    private JwtUsernamePasswordAuthenticationFilter jwtUsernamePasswordAuthenticationFilter() throws Exception {
        JwtUsernamePasswordAuthenticationFilter filter = new JwtUsernamePasswordAuthenticationFilter(
                authenticationManager(),
                loginSuccessHandler,
                loginFailureHandler
        );
        return filter;
    }

    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    // 문자셋 필터
    public CharacterEncodingFilter encodingFilter() {
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);
        return encodingFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // AuthenticationManager 빈 등록
    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return super.authenticationManager();
    }

    // cross origin 접근 허용
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    // 접근 제한 무시 경로 설정 - resource
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/assets/**", "/*", "/api/member/**",
            // Swagger 관련 url은 보안에서 제외
            "/swagger-ui.html", "/webjars/**", "/swagger-resources/**", "/v2/api-docs"
        );
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 인증 필터 등록하는 방법 1 => 특정 인증 필터 지정해서 filter 처리하는 방법
        http
            .addFilterBefore(encodingFilter(), CsrfFilter.class) // 문자 인코딩 처리 (기본 필터보다 먼저 실행)
            // 인증 관련 커스텀 필터 순서
            // ③ 로그인 시 JWT 발급
            .addFilterBefore(jwtUsernamePasswordAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            // ② 요청 시 토큰 유무 검사 및 인증 객체 설정
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // ① 만료된 토큰 등 예외 처리
            .addFilterBefore(authenticationErrorFilter, JwtAuthenticationFilter.class);

        // 인증 필터 등록하는 방법 2 => 등록된 순서에 따라 filter 위치가 지정됨.
//        // 한글 인코딩 필터 설정
//        http.addFilterBefore(encodingFilter(), CsrfFilter.class)
//            // 인증 에러 필터
//            .addFilterBefore(authenticationErrorFilter, UsernamePasswordAuthenticationFilter.class)
//            // Jwt 인증 필터
//            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
//            // 로그인 인증 필터
//            .addFilterBefore(jwtUsernamePasswordAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // 예외 처리 설정
        http
            .exceptionHandling()
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(accessDeniedHandler);

        http.httpBasic().disable()      // 기본 HTTP 인증 비활성화
            .csrf().disable()       // CSRF 비활성화
            .formLogin().disable()  // formLogin 비활성화   관련 필터 해제
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);    // 세션 생성 모드 설정

        http.authorizeRequests()    // 경로별 접근 권한 설정
            .antMatchers(HttpMethod.OPTIONS).permitAll()
            .antMatchers(HttpMethod.POST, "/api/member").authenticated()
            .antMatchers(HttpMethod.PUT, "/api/member", "/api/member/*/changepassword").authenticated()
            .antMatchers(HttpMethod.POST, "/api/board/**").authenticated()
            .antMatchers(HttpMethod.PUT, "/api/board/**").authenticated()
            .antMatchers(HttpMethod.DELETE, "/api/board/**").authenticated()
            .anyRequest().permitAll();
    }

    // Authentication Manager 구성
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // in memory user 정보 삭제 -> UserDetailsService와 같이 사용 불가
        auth
            .userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder());
    }
}
