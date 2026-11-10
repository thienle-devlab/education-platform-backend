package com.lethien.user_service.config;

import com.lethien.user_service.filter.InternalApiKeyFilter;
import com.lethien.user_service.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig — JWT-based stateless security cho User Service.
 *
 * Filter order:
 *   1. InternalApiKeyFilter  → chặn /internal/** nếu thiếu/sai API key
 *   2. JwtAuthenticationFilter → validate JWT cho các endpoint còn lại
 *
 * Endpoint access:
 *   PUBLIC     : Swagger, Actuator
 *   API KEY    : /internal/** (inter-service, no JWT needed)
 *   JWT        : tất cả endpoint còn lại
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final InternalApiKeyFilter internalApiKeyFilter;

    private static final String[] PUBLIC_URLS = {
            // Swagger / OpenAPI
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/actuator/health"
            // user-service KHÔNG có public auth endpoint
            // vì việc đăng ký/đăng nhập là của auth-service
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()

                        // Internal endpoints — bảo vệ bởi InternalApiKeyFilter
                        // permitAll ở đây vì filter đã reject trước khi vào Spring Security
                        .requestMatchers("/internal/**").permitAll()
                        .anyRequest().authenticated()  // tất cả API còn lại phải có JWT
                )

                // InternalApiKeyFilter chạy TRƯỚC JwtAuthenticationFilter
                // để /internal/** không bị check JWT
                .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                // TODO: Sau này thêm JWT filter vào đây để verify token từ auth-service
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
