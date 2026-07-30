package com.campusfruit.testing;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用 Testcontainers 的元注解。
 * <p>
 * 自动启动 MySQL Testcontainer 并配置 Spring 数据源。
 * 使用时标注在集成测试类上。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public @interface EnableTestContainers {

    /**
     * MySQL Docker 镜像名，默认 mysql:9.2。
     */
    String mysqlImage() default "mysql:9.2";

    /**
     * 数据库名。
     */
    String databaseName() default "campusfruit_test";

    /**
     * MySQL 用户名。
     */
    String username() default "test";

    /**
     * MySQL 密码。
     */
    String password() default "test";
}
