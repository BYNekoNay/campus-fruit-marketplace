package com.campusfruit.gateway.config;

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
                        .path("/api/auth/**", "/api/users/**", "/api/roles/**", "/api/me/**",
                                "/api/account/**", "/api/appeals/**", "/api/admin/users/**")
                        .filters(f -> f
                                .addRequestHeader("X-Trace-Id", UUID.randomUUID().toString())
                                .retry(2))
                        .uri("lb://identity-service"))

                // Discovery Service
                .route("discovery-service", r -> r
                        .path("/api/discovery/**", "/api/favorites/**")
                        .filters(f -> f
                                .addRequestHeader("X-Trace-Id", UUID.randomUUID().toString())
                                .retry(2))
                        .uri("lb://discovery-service"))

                // Review-specific store endpoints must precede generic store routing.
                .route("review-service", r -> r
                        .path("/api/reviews/**", "/api/ratings/**", "/api/reports/**",
                                "/api/admin/reports/**", "/api/admin/reviews/**", "/api/stores/*/reviews")
                        .filters(f -> f
                                .addRequestHeader("X-Trace-Id", UUID.randomUUID().toString())
                                .retry(2))
                        .uri("lb://review-service"))

                // Offer-specific store endpoints must precede generic store routing.
                .route("offer-service", r -> r
                        .path("/api/offers/**", "/api/coupons/**", "/api/promotions/**", "/api/flash-sales/**",
                                "/api/fruits/**", "/api/catalog/**", "/api/admin/fruits/**", "/api/stores/*/offers")
                        .filters(f -> f
                                .addRequestHeader("X-Trace-Id", UUID.randomUUID().toString())
                                .retry(2))
                        .uri("lb://offer-service"))

                // Merchant Service
                .route("merchant-service", r -> r
                        .path("/api/merchant/**", "/api/merchants/**", "/api/stores/**", "/api/products/**",
                                "/api/admin/merchants/**", "/api/admin/stores/**")
                        .filters(f -> f
                                .addRequestHeader("X-Trace-Id", UUID.randomUUID().toString())
                                .retry(2))
                        .uri("lb://merchant-service"))

                // Order Service
                .route("order-service", r -> r
                        .path("/api/orders/**", "/api/cart/**", "/api/payments/**", "/api/store/orders/**")
                        .filters(f -> f
                                .addRequestHeader("X-Trace-Id", UUID.randomUUID().toString())
                                .retry(2))
                        .uri("lb://order-service"))

                .build();
    }
}
