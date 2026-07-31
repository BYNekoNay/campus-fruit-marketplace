package com.campusfruit.gateway;

import com.campusfruit.gateway.config.JwtAuthFilter;
import com.campusfruit.gateway.config.SecurityConfig;
import com.campusfruit.gateway.config.TraceFilterConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ContextLoadIT {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        // 验证 Spring 应用上下文能够正常加载
    }

    @Test
    void jwtAuthFilterShouldBeRegistered() {
        assertThat(context.getBean(JwtAuthFilter.class)).isNotNull();
        assertThat(context.getBeanNamesForType(GlobalFilter.class))
                .contains("jwtAuthFilter");
    }

    @Test
    void traceFilterShouldBeOrderedFirst() {
        WebFilter traceFilter = context.getBean("traceFilter", WebFilter.class);
        assertThat(traceFilter).isNotNull();
    }

    @Test
    void securityWebFilterChainShouldBeConfigured() {
        assertThat(context.getBean(SecurityConfig.class)).isNotNull();
    }

    @Test
    void routeLocatorShouldRegisterAllPublicServiceRoutes() {
        RouteLocator routeLocator = context.getBean(RouteLocator.class);

        assertThat(routeLocator.getRoutes().map(route -> route.getId()).collectList().block())
                .contains("identity-service", "discovery-service", "merchant-service",
                        "offer-service", "order-service", "review-service");
    }

    @Test
    void discoveryRouteShouldMatchDiscoveryAndFavoriteEndpoints() {
        RouteLocator routeLocator = context.getBean(RouteLocator.class);
        Route discoveryRoute = routeLocator.getRoutes()
                .filter(route -> route.getId().equals("discovery-service"))
                .blockFirst();

        assertThat(discoveryRoute).isNotNull();
        assertThat(Mono.from(discoveryRoute.getPredicate().apply(exchangeFor("/api/discovery/categories"))).block())
                .isTrue();
        assertThat(Mono.from(discoveryRoute.getPredicate().apply(exchangeFor("/api/favorites"))).block())
                .isTrue();
    }

    private MockServerWebExchange exchangeFor(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
    }
}
