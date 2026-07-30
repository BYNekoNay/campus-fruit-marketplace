package com.campusfruit.gateway;

import com.campusfruit.gateway.config.JwtAuthFilter;
import com.campusfruit.gateway.config.SecurityConfig;
import com.campusfruit.gateway.config.TraceFilterConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.WebFilter;

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
}
