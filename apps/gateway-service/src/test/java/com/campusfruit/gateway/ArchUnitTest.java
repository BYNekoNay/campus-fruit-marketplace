package com.campusfruit.gateway;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit 架构测试 — gateway-service。
 */
class ArchUnitTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.campusfruit.gateway");
    }

    /**
     * 服务包不得引用其他服务的内部包。
     */
    @Test
    void internalPackageShouldNotLeak() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.campusfruit.gateway..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.campusfruit.identity..",
                        "com.campusfruit.merchant..",
                        "com.campusfruit.offer..",
                        "com.campusfruit.order..",
                        "com.campusfruit.review..",
                        "com.campusfruit.discovery.."
                );
        rule.check(importedClasses);
    }

    /**
     * Gateway 的 config 层不得依赖其他服务的业务包。
     */
    @Test
    void gatewayConfigShouldNotDependOnBusinessServices() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.campusfruit.gateway.config..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.campusfruit.identity..",
                        "com.campusfruit.merchant..",
                        "com.campusfruit.offer..",
                        "com.campusfruit.order..",
                        "com.campusfruit.review.."
                );
        rule.check(importedClasses);
    }
}
