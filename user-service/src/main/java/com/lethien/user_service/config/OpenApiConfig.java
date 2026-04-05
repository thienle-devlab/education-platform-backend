package com.lethien.user_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI Configuration for User Service
 * Most endpoints require JWT Bearer token
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
                // API Info
                .info(new Info()
                        .title("User Service API")
                        .description("User Management Service for Education Platform")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Education Platform Team")
                                .email("support@educationplatform.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))

                // Servers
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api/users")
                                .description("API Gateway (Recommended)"),
                        new Server()
                                .url("http://localhost:8082" + contextPath)
                                .description("Direct Access (Development)")))

                // Security Scheme (JWT Bearer)
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT access token from Auth Service (without 'Bearer' prefix)")))

                // Apply security globally (all endpoints require auth by default)
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName));
    }
}
