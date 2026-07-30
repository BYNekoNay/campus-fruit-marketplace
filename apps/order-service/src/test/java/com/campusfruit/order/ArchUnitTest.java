package com.campusfruit.order;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit 架构测试 — order-service。
 * <p>
 * 验证分层架构约束：
 * 1. 不依赖其他微服务的包
 * 2. controller 不直接访问 repository
 * 3. entity 不依赖 service/controller
 */
class ArchUnitTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.campusfruit.order");
    }

    /**
     * 规则1：服务包不得引用其他服务的包。
     */
    @Test
    void shouldNotDependOnOtherServicePackages() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.campusfruit.order..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.campusfruit.gateway..",
                        "com.campusfruit.identity..",
                        "com.campusfruit.merchant..",
                        "com.campusfruit.offer..",
                        "com.campusfruit.review..",
                        "com.campusfruit.discovery.."
                );
        rule.check(importedClasses);
    }

    /**
     * 规则2：Controller 层不直接访问 Repository。
     */
    @Test
    void controllerShouldNotAccessRepository() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat()
                .resideInAPackage("..repository..")
                .allowEmptyShould(true);
        rule.check(importedClasses);
    }

    /**
     * 规则3：entity 不依赖 service 和 controller。
     */
    @Test
    void entityShouldNotDependOnServiceOrController() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..entity..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..service..", "..controller..")
                .allowEmptyShould(true);
        rule.check(importedClasses);
    }
}
