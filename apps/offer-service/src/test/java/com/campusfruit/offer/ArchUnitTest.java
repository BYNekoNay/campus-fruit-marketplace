package com.campusfruit.offer;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit 架构测试 -- offer-service。
 */
class ArchUnitTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.campusfruit.offer");
    }

    /**
     * 规则1：服务包不得引用其他服务的包。
     */
    @Test
    void shouldNotDependOnOtherServicePackages() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.campusfruit.offer..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.campusfruit.gateway..",
                        "com.campusfruit.identity..",
                        "com.campusfruit.merchant..",
                        "com.campusfruit.order..",
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
     * 规则3：Service 层不依赖 Controller 层。
     */
    @Test
    void serviceShouldNotDependOnController() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..service..")
                .should().dependOnClassesThat()
                .resideInAPackage("..controller..")
                .allowEmptyShould(true);
        rule.check(importedClasses);
    }
}
