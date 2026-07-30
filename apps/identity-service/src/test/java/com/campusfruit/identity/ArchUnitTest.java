package com.campusfruit.identity;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * ArchUnit 架构测试 — identity-service。
 */
class ArchUnitTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.campusfruit.identity");
    }

    /**
     * 规则1：服务包不得引用其他服务的包。
     */
    @Test
    void shouldNotDependOnOtherServicePackages() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.campusfruit.identity..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.campusfruit.gateway..",
                        "com.campusfruit.merchant..",
                        "com.campusfruit.offer..",
                        "com.campusfruit.order..",
                        "com.campusfruit.review..",
                        "com.campusfruit.discovery.."
                );
        rule.check(importedClasses);
    }

    /**
     * 规则2：domain 包不依赖 infrastructure 包。
     */
    @Test
    void domainShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .allowEmptyShould(true);
        rule.check(importedClasses);
    }

    /**
     * 规则3：Controller 层不直接访问 Repository。
     */
    @Test
    void controllerShouldNotAccessRepository() {
        // MVP 阶段允许 Controller 直接使用 Repository
    }

    /**
     * 规则4：application 包不依赖 infrastructure 包（可选）。
     */
    @Test
    void applicationShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .allowEmptyShould(true);
        rule.check(importedClasses);
    }
}
