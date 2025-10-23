package com.smartcane.point.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

//    @Bean
//    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                .cors(Customizer.withDefaults())
//                .headers(h -> h.frameOptions(frame -> frame.sameOrigin())) // (필요시 H2 콘솔 등)
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
//
//                        // 앱 자체 경로
//                        .requestMatchers(
//                                "/swagger-ui/**", "/v3/api-docs/**",
//                                "/api/healthz", "/actuator/health"
//                        ).permitAll()
//
//                        // ALB가 붙여서 들어오는 경로 (users 프리픽스)
//                        .requestMatchers(
//                                "/points/swagger-ui/**", "/points/v3/api-docs/**",
//                                "/points/api/healthz", "/points/actuator/health"
//                        ).permitAll()
//
//                        .anyRequest().authenticated()
//                )
//                // 로그인 폼 대신 Basic 인증만(리다이렉트 없음)
//                .httpBasic(Customizer.withDefaults());
//
//        // formLogin()은 넣지 마세요 (넣으면 /login 리다이렉트 생김)
//        return http.build();
//    }

//    @Bean
//    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(auth -> auth
//                        // Swagger / Springdoc
//                        .requestMatchers(
//                                "/api/healthz",
//                                "/swagger-ui/**",
//                                "/v3/api-docs/**",
//                                "/actuator/health"
//                        ).permitAll()
//
//                        // ALB 프리픽스가 붙은 경로(중요)
//                        .requestMatchers(
//                                "/points/api/healthz",
//                                "/points/swagger-ui/**",
//                                "/points/v3/api-docs/**",
//                                "/points/actuator/health"
//                        ).permitAll()
//
//                        // API 전부 공개(개발용): 필요 없으면 제거하고 인증 설정해도 됨
//                        .requestMatchers("/api/**","/points/api/**").permitAll()
//                        // 나머지는 인증 요구
//                        .anyRequest().authenticated()
//                )
//                // 개발 편의를 위해 httpBasic만 켬 (원하면 제거)
//                .httpBasic(Customizer.withDefaults());
//
//        return http.build();
//    }


    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 전체 비활성(세션 없는 API/문서용이면 이게 단순하고 안전)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/docs", "/docs/**",
                                "/actuator/health",
                                "/api/**",
                                "/points/**"// 공개하려는 엔드포인트면 유지
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // 기본 인증/폼 로그인 비활성
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    // CORS (필요시 ALB 도메인만 허용하도록 바꾸세요)
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("*")); // 예: "https://admin.your-domain.com"
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
