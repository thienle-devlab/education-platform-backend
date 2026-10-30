package com.lethien.course_service.config;

import com.lethien.course_service.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig — JWT-based stateless security cho Course Service.
 *
 * Thay đổi so với version cũ:
 *   + @EnableMethodSecurity → cho phép @PreAuthorize trên controller/service
 *   + Role-based access control qua Spring Security authorities
 *     (không cần gọi UserServiceClient chỉ để check role nữa)
 *
 * Role hierarchy:
 *   ADMIN      → có tất cả quyền
 *   INSTRUCTOR → tạo/sửa/xóa course của mình
 *   STUDENT    → enroll, học bài, làm bài tập
 *   (public)   → xem catalog, tìm kiếm
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // Bật @PreAuthorize cho role-based access (INSTRUCTOR, ADMIN)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String[] PUBLIC_URLS = {
            // Swagger / OpenAPI
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",

            // Health check
            "/actuator/health",

            // Public course endpoints — không cần đăng nhập
//            "/courses",                    // GET danh sách khóa học
//            "/courses/{id}",               // GET chi tiết khóa học
//            "/courses/search",             // GET tìm kiếm khóa học
//            "/courses/{id}/sections",      // GET danh sách chương
//            "/courses/{id}/reviews",       // GET đánh giá khóa học
    };

    // Public GET — catalog & course detail (không cần JWT)
    private static final String[] PUBLIC_GET_URLS = {
            "/",
            "/search",
            "/{id}",
            "/slug/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Swagger + Actuator
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        // Public catalog — GET only, no JWT needed
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_URLS).permitAll()
                        // Instructor-only endpoints — check ở đây hoặc @PreAuthorize
                        .requestMatchers(HttpMethod.POST, "/").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/**").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers("/*/publish").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers("/*/archive").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers("/instructor/**").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/*/reviews").permitAll()
                        .requestMatchers(HttpMethod.GET, "/*/sections").permitAll()
                        .requestMatchers(HttpMethod.POST, "/*/sections").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/*/sections/reorder").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/sections/**").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/sections/**").hasAnyRole("INSTRUCTOR", "ADMIN")
                        // Tất cả còn lại — cần JWT (bất kỳ role nào)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }
}
