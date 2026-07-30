package com.campusfruit.testing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 测试数据工厂工具类。
 * <p>
 * 提供常用的测试用常量和方法，用于快速构造测试数据。
 */
public final class TestDataFactory {

    private TestDataFactory() {
        // 工具类禁止实例化
    }

    // --- 通用 ID 常量 ---

    public static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID TEST_MERCHANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID TEST_STORE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final UUID TEST_ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    // --- 常量 ---

    public static final String TEST_USER_EMAIL = "testuser@campusfruit.com";
    public static final String TEST_MERCHANT_NAME = "测试水果店铺";
    public static final BigDecimal TEST_PRODUCT_PRICE = new BigDecimal("29.90");

    /**
     * 生成一个随机的 UUID 字符串。
     */
    public static String randomUuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * 当前时间 UTC。
     */
    public static Instant now() {
        return Instant.now();
    }

    /**
     * 过去指定秒数的时间。
     */
    public static Instant secondsAgo(long seconds) {
        return Instant.now().minusSeconds(seconds);
    }

    /**
     * 未来指定秒数的时间。
     */
    public static Instant secondsLater(long seconds) {
        return Instant.now().plusSeconds(seconds);
    }
}
