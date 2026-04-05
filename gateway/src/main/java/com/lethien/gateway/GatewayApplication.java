package com.lethien.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

/**
 * API Gateway Application
 * Single entry point for all microservices
 */
@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

    /**
     * Define routes programmatically (optional - can use application.yml instead)
     * This is just an example, you can configure everything in application.yml
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
        // Health check route
                .route("gateway-health", r -> r
                        .path("/health")
                        .uri("http://localhost:8080"))
                .build();
    }

}
