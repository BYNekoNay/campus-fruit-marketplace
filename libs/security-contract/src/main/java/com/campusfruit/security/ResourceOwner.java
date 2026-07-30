package com.campusfruit.security;

import java.util.Set;

/**
 * 资源所有者接口。
 * <p>
 * 表示当前请求的认证主体（用户/服务），用于跨服务的资源归属判断。
 */
public interface ResourceOwner {

    /**
     * 获取当前用户/服务的唯一标识。
     *
     * @return 用户 ID（字符串形式）
     */
    String getUserId();

    /**
     * 获取当前用户/服务所拥有的角色集合。
     *
     * @return 角色集合（包含 ROLE_ 前缀）
     */
    Set<String> getRoles();

    /**
     * 判断当前用户是否拥有指定角色。
     *
     * @param role 角色名（如 {@link SecurityConstants#ROLE_ADMIN}）
     * @return true 如果拥有该角色
     */
    default boolean hasRole(String role) {
        return getRoles() != null && getRoles().contains(role);
    }

    /**
     * 判断当前用户是否拥有任意一个指定角色。
     *
     * @param roles 角色名列表
     * @return true 如果拥有任意一个
     */
    default boolean hasAnyRole(String... roles) {
        if (getRoles() == null || roles == null) {
            return false;
        }
        for (String role : roles) {
            if (getRoles().contains(role)) {
                return true;
            }
        }
        return false;
    }
}
