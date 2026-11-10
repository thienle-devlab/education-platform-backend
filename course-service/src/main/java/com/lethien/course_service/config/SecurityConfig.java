package com.lethien.course_service.config;

import com.lethien.course_service.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
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

import java.io.IOException;


/**
 * SecurityConfig (bản tổng hợp) — thay THẾ HOÀN TOÀN file hiện tại.
 *
 * Gom toàn bộ rule của catalog, review, section, lesson, exercise vào một chỗ
 * thay cho các file "SecurityConfigAdditions" rời rạc trước đó.
 *
 * Lưu ý quan trọng: pattern tính theo path SAU context-path (/api/courses).
 *   Controller map /courses/{id}/sections có 3 segment, nên pattern cũng phải có đủ
 *   3 segment: "courses", một dấu sao, "sections". Pattern chỉ có 2 segment
 *   (một dấu sao rồi "sections") sẽ KHÔNG khớp. Xem các mảng *_URLS bên dưới.
 *
 * Thứ tự rule rất quan trọng: rule khớp đầu tiên được áp dụng.
 *
 * Quyền ghi nội dung (course/section/lesson/exercise): chỉ INSTRUCTOR.
 * ADMIN không có quyền tạo/sửa/xóa nội dung — đúng quyết định "ADMIN chỉ kiểm duyệt".
 * Service vẫn kiểm tra thêm đúng chủ sở hữu course.
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
            "/slug/*",
            "/courses/*/reviews",
            "/courses/*/curriculum",
            "/courses/*/sections",
            "/sections/*/lessons",
            "/lessons/*"
    };

    private static final String[] INSTRUCTOR_GET_URLS = {
            "/instructor/me"
    };

    private static final String[] INSTRUCTOR_POST_URLS = {
            "/", "/*/publish", "/*/archive",
            "/courses/*/sections", "/courses/*/sections/reorder",
            "/sections/*/lessons", "/sections/*/lessons/reorder",
            "/lessons/*/exercises", "/lessons/*/exercises/reorder"
    };

    private static final String[] INSTRUCTOR_PATCH_URLS = {
            "/*", "/sections/*", "/lessons/*", "/lessons/*/skill-content", "/exercises/*"
    };

    private static final String[] INSTRUCTOR_DELETE_URLS = {
            "/*", "/sections/*", "/lessons/*", "/exercises/*"
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
                        .requestMatchers(HttpMethod.GET,    INSTRUCTOR_GET_URLS).hasRole("INSTRUCTOR")
                        .requestMatchers(HttpMethod.POST,   INSTRUCTOR_POST_URLS).hasRole("INSTRUCTOR")
                        .requestMatchers(HttpMethod.PATCH,  INSTRUCTOR_PATCH_URLS).hasRole("INSTRUCTOR")
                        .requestMatchers(HttpMethod.DELETE, INSTRUCTOR_DELETE_URLS).hasRole("INSTRUCTOR")

                        // Tất cả còn lại — cần JWT (bất kỳ role nào)
                        .anyRequest().authenticated()
                )

                // 401 = chưa đăng nhập / token hết hạn (client nên refresh token)
                // 403 = đã đăng nhập nhưng không đủ quyền
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                writeError(res, HttpServletResponse.SC_UNAUTHORIZED,
                                        "UNAUTHORIZED", "Authentication required"))
                        .accessDeniedHandler((req, res, e) ->
                                writeError(res, HttpServletResponse.SC_FORBIDDEN,
                                        "ACCESS_DENIED", "You do not have permission to perform this action"))
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }

    private static void writeError(HttpServletResponse res, int status, String code, String message) throws
            IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"status\":" + status
                + ",\"success\":false,\"message\":\"" + message + "\""
                + ",\"error\":{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}}");
    }
}
