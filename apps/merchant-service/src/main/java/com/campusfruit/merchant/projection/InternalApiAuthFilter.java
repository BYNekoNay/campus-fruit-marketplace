package com.campusfruit.merchant.projection;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 内部 API 认证过滤器。
 * 验证 X-Internal-API-Key header 是否匹配配置值。
 */
@Component
@Order(1)
public class InternalApiAuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(InternalApiAuthFilter.class);

    private static final String API_KEY_HEADER = "X-Internal-API-Key";

    @Value("${app.internal.api-key}")
    private String internalApiKey;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        // 只拦截内部 API 路径
        if (!path.startsWith("/api/internal/")) {
            chain.doFilter(request, response);
            return;
        }

        String apiKey = httpRequest.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.isBlank() || !internalApiKey.equals(apiKey)) {
            log.warn("Internal API access denied: invalid or missing API key for path {}", path);
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\": \"Forbidden: invalid internal API key\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
