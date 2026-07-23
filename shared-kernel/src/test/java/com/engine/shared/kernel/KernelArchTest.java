package com.engine.shared.kernel;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture tests for the shared-kernel module.
 *
 * <p>The kernel is the one dependency that every bounded context is allowed to
 * import from; therefore the kernel must itself remain free of any framework
 * or delivery mechanism (Spring, JPA, Jackson, SLF4J, Hibernate). These rules
 * fail the build instantly if that purity is ever broken.
 */
@AnalyzeClasses(packages = "com.engine.shared")
class KernelArchTest {

    @ArchTest
    static final ArchRule kernelMustNotDependOnSpring =
            noClasses().should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .because("the shared kernel must stay framework-agnostic");

    @ArchTest
    static final ArchRule kernelMustNotDependOnJpaOrHibernate =
            noClasses().should().dependOnClassesThat()
                    .resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
                    .because("persistence is an adapter concern, never a kernel concern");

    @ArchTest
    static final ArchRule kernelMustNotDependOnJackson =
            noClasses().should().dependOnClassesThat().resideInAPackage("com.fasterxml.jackson..")
                    .because("serialization is an adapter concern, never a kernel concern");

    @ArchTest
    static final ArchRule kernelMustNotDependOnSlf4j =
            noClasses().should().dependOnClassesThat().resideInAPackage("org.slf4j..")
                    .because("logging is an infrastructure concern, never a kernel concern");
}