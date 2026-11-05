package ru.phyllosedis.flashsale.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutes {

    @Bean
    public RouteLocator routes(
            RouteLocatorBuilder builder,
            @Value("${app.routes.user-uri:http://localhost:8081}") String userServiceUri,
            @Value("${app.routes.payment-uri:http://localhost:8082}") String paymentServiceUri) {
        return builder.routes()
                .route("user-service", route -> route
                        .path("/api/v1/users/**")
                        .uri(userServiceUri))
                .route("payment-service", route -> route
                        .path("/api/v1/payments/**")
                        .uri(paymentServiceUri))
                .build();
    }
}
