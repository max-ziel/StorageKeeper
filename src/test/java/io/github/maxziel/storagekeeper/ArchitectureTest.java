package io.github.maxziel.storagekeeper;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.maxziel.storagekeeper.application.port.in.UpdateShoppingListUseCase;

@AnalyzeClasses(packages = "io.github.maxziel.storagekeeper", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule portsAndAdapters = onionArchitecture()
            .domainModels("..domain..")
            .applicationServices("..application..")
            .adapter("web", "..adapter.in.web..")
            .adapter("persistence", "..adapter.out.persistence..")
            .adapter("events-in", "..adapter.in.event..")
            .adapter("events-out", "..adapter.out.event..")
            .adapter("security", "..adapter.out.security..")
            // Technical beans (e.g. the Clock); like an adapter, nothing else may depend on it.
            .adapter("config", "..config..")
            .withOptionalLayers(true);

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..")
            .allowEmptyShould(true);

    /** Spring only for wiring ({@code @Service}, {@code @Component}) and transactions, nothing else. */
    @ArchTest
    static final ArchRule applicationUsesSpringOnlyForWiringAndTransactions = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat(resideInAPackage("org.springframework..")
                    .and(not(resideInAnyPackage(
                            "org.springframework.stereotype..",
                            "org.springframework.transaction.annotation.."))))
            .orShould().dependOnClassesThat().resideInAPackage("jakarta..");

    /**
     * {@link UpdateShoppingListUseCase} does not check the household; it may only be called after a change that
     * was already checked, i.e. by the domain event listener.
     */
    @ArchTest
    static final ArchRule shoppingListIsOnlyUpdatedFromDomainEvents = noClasses()
            .that().resideOutsideOfPackages("..application..", "..adapter.in.event..")
            .should().dependOnClassesThat().belongToAnyOf(UpdateShoppingListUseCase.class);
}