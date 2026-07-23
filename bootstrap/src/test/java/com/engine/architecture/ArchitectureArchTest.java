package com.engine.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Cross-module architecture guardrails.
 *
 * <p>Bootstrapped from the {@code bootstrap} module because at test time the
 * bootstrap classpath sees every bounded context's main classes (identity, order,
 * payment, shared-kernel) and ArchUnit can therefore run cross-module rules
 * against the whole project graph in one place.
 *
 * <p>These rules are intentionally vacuously green at the start: every bounded
 * context still has nothing but a {@code package-info.java}. As soon as real
 * classes are added in later stages, any drift from the hexagonal contract will
 * cause a build failure here &mdash; the guardrails work forever after.
 */
@AnalyzeClasses(packages = "com.engine", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureArchTest {

    @ArchTest
    static final ArchRule domainLayerMustNotDependOnAnyFramework =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "org.hibernate..",
                            "com.fasterxml.jackson..",
                            "org.slf4j.."
                    )
                    .because("domain must be pure Java, depending only on the shared kernel");

    @ArchTest
    static final ArchRule domainLayerMustNotDependOnAdapters =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..adapters..")
                    .because("the hexagonal dependency rule forbids the domain depending on its adapters");

    @ArchTest
    static final ArchRule domainLayerMustNotDependOnApplication =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..application..")
                    .because("the domain layer is the innermost ring and must not depend on the application layer that orchestrates it; application depends on domain, never the reverse");

    @ArchTest
    static final ArchRule applicationLayerMustNotDependOnAdapters =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..adapters..")
                    .because("the application layer may only depend on the domain layer, not the adapters that implement its ports");

    @ArchTest
    static final ArchRule inboundAdaptersMustNotDependOnOutboundAdapters =
            noClasses().that().resideInAPackage("..adapters.in..")
                    .should().dependOnClassesThat().resideInAPackage("..adapters.out..")
                    .because("inbound and outbound adapters communicate only via the application layer's use cases and ports");

    @ArchTest
    static final ArchRule orderContextMustNotImportOtherContexts =
            noClasses().that().resideInAPackage("com.engine.order..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("com.engine.identity..", "com.engine.payment..")
                    .because("bounded contexts communicate only through domain events published over RabbitMQ");

    @ArchTest
    static final ArchRule paymentContextMustNotImportOtherContexts =
            noClasses().that().resideInAPackage("com.engine.payment..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("com.engine.identity..", "com.engine.order..")
                    .because("bounded contexts communicate only through domain events published over RabbitMQ");

    @ArchTest
    static final ArchRule identityContextMustNotImportOtherContexts =
            noClasses().that().resideInAPackage("com.engine.identity..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("com.engine.order..", "com.engine.payment..")
                    .because("bounded contexts communicate only through domain events published over RabbitMQ");
}