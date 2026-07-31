package com.campusfruit.gateway.config;

import com.campusfruit.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.http.server.PathContainer;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JWT 认证全局过滤器。
 * <p>
 * 在 Gateway 层对请求进行 JWT 格式检查和声明提取，不验证签名。
 * 认证通过后将 userId 和 roles 通过 X-User-Id / X-User-Roles header 传递给下游服务。
 * <p>
 * order=1，排在 TraceFilter (order=0) 之后。
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final PathPatternParser PATH_PATTERN_PARSER = new PathPatternParser();

    private final List<PathPattern> whitelist;

    public JwtAuthFilter(@Value("${app.auth.whitelist}") List<String> whitelist) {
        this.whitelist = whitelist.stream().map(PATH_PATTERN_PARSER::parse).toList();
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 白名单路径直接放行
        if (isWhitelisted(path)) {
            log.debug("Whitelisted path, skipping auth: {}", path);
            return chain.filter(exchange);
        }

        // 提取 Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
            log.warn("Missing or invalid Authorization header, path={}", path);
            return unauthorized(exchange, "UNAUTHORIZED", "未提供认证令牌");
        }

        String token = authHeader.substring(7).trim();

        // 使用 security-contract 库解析 JWT（仅格式检查，不验证签名）
        Optional<String> userIdOpt = JwtTokenProvider.getUserIdFromToken(token);
        if (userIdOpt.isEmpty() || userIdOpt.get().isBlank()) {
            log.warn("JWT missing 'sub' claim or malformed, path={}", path);
            return unauthorized(exchange, "UNAUTHORIZED", "令牌格式错误：缺少用户标识");
        }

        String userId = userIdOpt.get();
        List<String> rolesList = JwtTokenProvider.getRolesFromToken(token);
        String roles = String.join(",", rolesList);

        // 确保 traceId 存在（由 TraceFilter 在 order=0 生成）
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        // 将用户信息写入请求头传递给下游
        var mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .header("X-User-Roles", roles)
                .header(TRACE_ID_HEADER, traceId)
                .build();

        var mutatedExchange = exchange.mutate().request(mutatedRequest).build();

        log.debug("Auth success: user={}, roles={}, path={}, traceId={}",
                maskUserId(userId), roles, path, traceId);

        return chain.filter(mutatedExchange);
    }

    /**
     * 检查路径是否在白名单中。
     */
    private boolean isWhitelisted(String path) {
        PathContainer pathContainer = PathContainer.parsePath(path);
        return whitelist.stream().anyMatch(pattern -> pattern.matches(pathContainer));
    }

    /**
     * 返回 401 JSON 错误响应（ApiError 结构）。
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String errorCode, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);

        String body = String.format(
                "{\"errorCode\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"%s}",
                errorCode,
                message,
                Instant.now().toString(),
                traceId != null && !traceId.isBlank()
                        ? ",\"traceId\":\"" + traceId + "\""
                        : ""
        );

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * 脱敏处理：只显示用户 ID 前两位和后两位。
     */
    private String maskUserId(String userId) {
        if (userId == null || userId.length() <= 4) {
            return "***";
        }
        return userId.substring(0, 2) + "***" + userId.substring(userId.length() - 2);
    }
}
