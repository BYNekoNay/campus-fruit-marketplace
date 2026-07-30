package com.campusfruit.merchant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * 方法级安全配置。
 * 启用 @PreAuthorize / @PostAuthorize 等注解。
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
