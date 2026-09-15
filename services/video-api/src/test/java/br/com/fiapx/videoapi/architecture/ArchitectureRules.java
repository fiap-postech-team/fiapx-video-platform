package br.com.fiapx.videoapi.architecture;

import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

final class ArchitectureRules {

    private ArchitectureRules() {
    }

    static ArchRule coreIsIndependent() {
        return noClasses().that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..",
                "jakarta.persistence..",
                "org.springframework.amqp..",
                "io.minio..",
                "..adapter.."
            ).allowEmptyShould(true);
    }

    static ArchRule technicalBucketsAreForbidden() {
        return noClasses().should().resideInAnyPackage("..controller..", "..service..", "..repository..");
    }

    static ArchRule coreDoesNotDependOnAdapters() {
        return noClasses().that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..")
            .allowEmptyShould(true);
    }

    static ArchRule dependenciesAreImmutable() {
        return fields().that().areNotStatic().should().beFinal();
    }

    static ArchRule fieldInjectionIsForbidden() {
        return noFields().should().beAnnotatedWith(
            "org.springframework.beans.factory.annotation.Autowired"
        );
    }

    static ArchRule mediaAndNotificationDependenciesAreForbidden() {
        return noClasses().should().dependOnClassesThat().resideInAnyPackage(
            "java.awt..",
            "jakarta.mail..",
            "br.com.fiapx.videoprocessor..",
            "br.com.fiapx.notificationworker.."
        );
    }
}
