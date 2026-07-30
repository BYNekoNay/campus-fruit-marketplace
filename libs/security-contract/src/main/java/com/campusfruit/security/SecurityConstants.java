package com.campusfruit.security;

/**
 * 安全角色常量。
 * <p>
 * 定义系统中所有的角色名称，使用 Spring Security 的 ROLE_ 前缀约定。
 */
public final class SecurityConstants {

    private SecurityConstants() {
        // 常量类禁止实例化
    }

    /** 普通用户（消费者） */
    public static final String ROLE_USER = "ROLE_USER";

    /** 商家/店主 */
    public static final String ROLE_MERCHANT = "ROLE_MERCHANT";

    /** 店铺员工 */
    public static final String ROLE_STORE_STAFF = "ROLE_STORE_STAFF";

    /** 系统管理员 */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /** 运营人员 */
    public static final String ROLE_OPERATOR = "ROLE_OPERATOR";

    /** 所有角色列表 */
    public static final String[] ALL_ROLES = {
            ROLE_USER,
            ROLE_MERCHANT,
            ROLE_STORE_STAFF,
            ROLE_ADMIN,
            ROLE_OPERATOR
    };

    // --- 权限路径常量 ---

    /** 认证放行路径 */
    public static final String AUTH_WHITELIST = "/api/auth/**";

    /** 健康检查路径 */
    public static final String ACTUATOR_HEALTH = "/actuator/health";

    /** API 前缀 */
    public static final String API_PREFIX = "/api/";

    /** 管理 API 前缀 */
    public static final String ADMIN_API_PREFIX = "/api/admin/**";
}
