package com.aterrizAR.backend.architecture

import com.aterrizAR.backend.PingController
import com.aterrizAR.backend.PingLogRepository
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.Repository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate

class ArchitectureTest {
    @Test
    fun `los controllers no acceden directamente a repositorios`() {
        classes().that().areAnnotatedWith(RestController::class.java)
            .should(avoidDependencies("no acceder directamente a repositorios", PingLogRepository::class.java) {
                it.isAssignableTo(Repository::class.java)
            })
            .check(backendClasses)
    }

    @Test
    fun `los controllers no implementan persistencia`() {
        noClasses().that().areAnnotatedWith(RestController::class.java)
            .should().dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..", "java.sql..", "javax.sql..", "org.springframework.jdbc..")
            .check(backendClasses)
    }

    @Test
    fun `los controllers delegan las llamadas HTTP salientes`() {
        classes().that().areAnnotatedWith(RestController::class.java)
            .should(avoidDependencies("delegar las llamadas HTTP salientes", RestTemplate::class.java) {
                it.packageName.startsWith("org.springframework.web.client") ||
                    it.packageName.startsWith("org.springframework.web.reactive.function.client") ||
                    it.packageName.startsWith("java.net.http")
            })
            .check(backendClasses)
    }

    @Test
    fun `los services no dependen de controllers`() {
        noClasses().that().areAnnotatedWith(Service::class.java)
            .should().dependOnClassesThat().areAnnotatedWith(RestController::class.java)
            .check(backendClasses)
    }

    @Test
    fun `los repositorios no dependen de services ni controllers`() {
        noClasses().that().areAssignableTo(Repository::class.java)
            .should().dependOnClassesThat().areAnnotatedWith(Service::class.java)
            .check(backendClasses)
        noClasses().that().areAssignableTo(Repository::class.java)
            .should().dependOnClassesThat().areAnnotatedWith(RestController::class.java)
            .check(backendClasses)
    }

    @Test
    fun `los repositorios son interfaces Spring Data JPA`() {
        classes().that().haveSimpleNameEndingWith("Repository")
            .should().beInterfaces()
            .andShould().beAssignableTo(JpaRepository::class.java)
            .check(backendClasses)
    }

    @Test
    fun `el modelo no depende de otras capas del backend`() {
        classes().that().resideInAPackage("com.aterrizAR.backend.model..")
            .should().onlyDependOnClassesThat(
                resideOutsideOfPackage("com.aterrizAR.backend..")
                    .or(resideInAPackage("com.aterrizAR.backend.model..")),
            )
            .check(backendClasses)
    }

    @Test
    fun `las dependencias no se inyectan mediante campos`() {
        noFields().should().beAnnotatedWith(Autowired::class.java)
            .check(backendClasses)
    }

    companion object {
        private val backendClasses = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("com.aterrizAR.backend")

        private fun avoidDependencies(
            description: String,
            legacyTarget: Class<*>,
            forbiddenTarget: (JavaClass) -> Boolean,
        ): ArchCondition<JavaClass> = object : ArchCondition<JavaClass>(description) {
            override fun check(item: JavaClass, events: ConditionEvents) {
                item.directDependenciesFromSelf.filter { forbiddenTarget(it.targetClass) }.forEach { dependency ->
                    // Excepciones exactas del ping documentadas en AGENTS.md.
                    val legacyPing = item.name == PingController::class.java.name &&
                        dependency.targetClass.name == legacyTarget.name
                    events.add(SimpleConditionEvent(dependency, legacyPing, dependency.description))
                }
            }
        }
    }
}
