package com.campusfruit.gateway.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Trace 传播过滤器。
 * <p>
 * 为每个请求生成或传播 traceId，注入到请求头和响应头。
 * order=0，排在 JwtAuthFilter (order=1) 之前，确保 traceId 在 filter 链的最前面生成。
 */
@Configuration
public class TraceFilterConfig {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_ID_KEY = "traceId";

    @Bean
    @Order(0)
    public WebFilter traceFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString();
            }

            // 写入 MDC 用于日志
            MDC.put(MDC_TRACE_ID_KEY, traceId);

            // 修改请求，确保 traceId 传播到下游
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(TRACE_ID_HEADER, traceId)
                    .build();
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();

            // 写入响应头
            mutatedExchange.getResponse().getHeaders().add(TRACE_ID_HEADER, traceId);

            return chain.filter(mutatedExchange)
                    .doFinally(signalType -> MDC.remove(MDC_TRACE_ID_KEY));
        };
    }
}
