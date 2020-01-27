package nu.fgv.register.server

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class ArchTest {

    @Test
    fun repositoriesShouldNotDependOnServiceLayer() {
        val importedClasses = ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("nu.fgv.register")

        noClasses()
            .that()
            .resideInAnyPackage("..repository..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..nu.fgv.register.service..")
            .because("Repositories should not depend on service layer")
            .check(importedClasses)
    }

    @Test
    fun servicesAndRepositoriesShouldNotDependOnWebLayer() {
        val importedClasses = ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("nu.fgv.register")

        noClasses()
            .that()
            .resideInAnyPackage("..service..")
            .or()
            .resideInAnyPackage("..repository..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..nu.fgv.register.web..")
            .because("Services and repositories should not depend on web layer")
            .check(importedClasses)
    }
}
