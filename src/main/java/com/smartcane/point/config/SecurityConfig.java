package com.smartcane.point.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .headers(h -> h.frameOptions(frame -> frame.sameOrigin())) // (필요시 H2 콘솔 등)
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 전부 공개할 엔드포인트
                        .requestMatchers(
                                "/api/**",
                                "/docs", "/swagger-ui/**", "/v3/api-docs/**",
                                "/actuator/health"
                        ).permitAll()

                        // 나머지는 보호
                        .anyRequest().authenticated()
                )
                // 로그인 폼 대신 Basic 인증만(리다이렉트 없음)
                .httpBasic(Customizer.withDefaults());

        // formLogin()은 넣지 마세요 (넣으면 /login 리다이렉트 생김)
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
