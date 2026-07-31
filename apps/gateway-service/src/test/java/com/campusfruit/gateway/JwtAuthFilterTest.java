package com.campusfruit.gateway;

import com.campusfruit.gateway.config.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthFilterTest {

    @Test
    void publicDiscoveryStatisticsShouldBypassJwtAuthentication() {
        JwtAuthFilter filter = new JwtAuthFilter(List.of("/api/discovery/stats/**"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/discovery/stats/42").build());
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.filter(exchange, ignored -> {
            forwarded.set(true);
            return Mono.empty();
        }).block();

        assertThat(forwarded).isTrue();
    }
}
