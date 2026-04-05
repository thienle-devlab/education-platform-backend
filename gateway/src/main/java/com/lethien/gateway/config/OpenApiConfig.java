package com.lethien.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * API Gateway Swagger Configuration
 * Aggregates Swagger docs from all microservices
 */
@Configuration
public class OpenApiConfig {

    private final RouteDefinitionLocator routeLocator;

    public OpenApiConfig(RouteDefinitionLocator routeLocator) {
        this.routeLocator = routeLocator;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
                .info(new Info()
                        .title("Education Platform API Gateway")
                        .description("Unified API documentation for all microservices")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Education Platform Team")
                                .email("support@educationplatform.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))

                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("API Gateway - Local"),
                        new Server()
                                .url("https://api.educationplatform.com")
                                .description("API Gateway - Production")))

                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token from /api/auth/login")))

                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName));
    }

    /**
     * Group APIs by service
     * Creates separate tabs in Swagger UI for each microservice
     */
    @Bean
    public GroupedOpenApi authServiceAPI() {
        return GroupedOpenApi.builder()
                .group("1. Authentication Service")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userServiceApi() {
        return GroupedOpenApi.builder()
                .group("2. User Service")
                .pathsToMatch("/api/users/**")
                .build();
    }

    /**
     * Gateway routes (metadata only, not API endpoints)
     */
    @Bean
    public GroupedOpenApi gatewayApi() {
        return GroupedOpenApi.builder()
                .group("0. Gateway Routes")
                .pathsToMatch("/actuator/**")
                .build();
    }
}
