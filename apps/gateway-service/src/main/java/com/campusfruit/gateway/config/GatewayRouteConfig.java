package com.campusfruit.gateway.config;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * Gateway 路由配置。
 * <p>
 * 将所有 /api/{service}/** 请求路由到对应的微服务实例（lb://{service}-service）。
 */
@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Identity Service
                .route("identity-service", r -> r
                        .path("/api/auth/**", "/api/users/**", "/api/roles/**")
                        .filters(f -> f
                                .addRequestHeader("X-Trace-Id", UUID.randomUUID().toString())
                                .retry(2))
                        .uri("lb://identity-service"))

                // Merchant Service
                .route("merchant-service", r -> r
                        .path("/api/merchants/**", "/api/stores/**", "/api/products/**")
                        .filters(f -> f
                                .addRequestHeader("X-Trace-Id", UUID.randomUUID().toString())
                                .retry(2))
                        .uri("lb://merchant-service"))

                // Offer Service
                .route("offer-service", r -> r
                        .path("/api/offers/**", "/api/coupons/**", "/api/promotions/**", "/api/flash-sales/**")
                        .filters(f -> f
                                .addRequestHeader("X-Trace-Id", UUID.randomUUID().toString())
                                .retry(2))
                        .uri("lb://offer-service"))

                // Order Service
                .route("order-service", r -> r
                        .path("/api/orders/**", "/api/cart/**", "/api/payments/**")
                        .filters(f -> f
                                .addRequestHeader("X-Trace-Id", UUID.randomUUID().toString())
                                .retry(2))
                        .uri("lb://order-service"))

                // Review Service
                .route("review-service", r -> r
                        .path("/api/reviews/**", "/api/ratings/**")
                        .filters(f -> f
                                .addRequestHeader("X-Trace-Id", UUID.randomUUID().toString())
                                .retry(2))
                        .uri("lb://review-service"))

                .build();
    }
}
