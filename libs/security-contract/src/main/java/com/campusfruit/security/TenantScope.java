package com.campusfruit.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 租户作用域注解。
 * <p>
 * 标记需要按租户（校园/校区/店铺）进行数据隔离的方法或类。
 * AOP 切面可根据此注解自动注入租户上下文。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TenantScope {

    /**
     * 租户 ID 的 SpEL 表达式。
     * 例如 {@code "#order.tenantId"} 或 {@code "#userId"}。
     * 为空时自动从当前上下文解析。
     */
    String value() default "";

    /**
     * 是否严格要求租户上下文必须存在。
     * 设为 false 时，跨租户管理员操作可跳过隔离。
     */
    boolean required() default true;
}
