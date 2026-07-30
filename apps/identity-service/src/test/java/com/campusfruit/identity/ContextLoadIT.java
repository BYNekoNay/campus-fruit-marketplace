package com.campusfruit.identity;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ContextLoadIT {

    @Test
    void contextLoads() {
        // 验证 Spring 应用上下文能够正常加载
    }
}
