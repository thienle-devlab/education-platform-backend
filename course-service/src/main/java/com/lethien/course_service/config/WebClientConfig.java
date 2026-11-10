package com.lethien.course_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClientConfig — expose WebClient.Builder bean.
 *
 * Spring Boot auto-configures WebClient.Builder nhưng cần khai báo
 * explicit để có thể inject vào UserServiceClient với custom config.
 *
 * NOTE: Course Service dùng Spring WebMVC (blocking) nên cần thêm
 * dependency spring-boot-starter-webflux vào pom.xml để có WebClient.
 * WebClient vẫn dùng được trong WebMVC — chỉ cần thêm dependency,
 * không cần migrate sang reactive stack.
 */
@Configuration
public class WebClientConfig {
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
