package com.campusfruit.gateway;

import com.campusfruit.gateway.config.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthFilterTest {

    @Test
    void publicDiscoveryStatisticsShouldBypassJwtAuthentication() {
        JwtAuthFilter filter = new JwtAuthFilter(List.of("/api/discovery/stats/**"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/discovery/stats/42")
                        .header("Authorization", "Bearer stale-token")
                        .build());
        AtomicBoolean forwarded = new AtomicBoolean();
        AtomicReference<String> forwardedAuthorization = new AtomicReference<>();

        filter.filter(exchange, forwardedExchange -> {
            forwarded.set(true);
            forwardedAuthorization.set(forwardedExchange.getRequest().getHeaders().getFirst("Authorization"));
            return Mono.empty();
        }).block();

        assertThat(forwarded).isTrue();
        assertThat(forwardedAuthorization).isNull();
    }
}
