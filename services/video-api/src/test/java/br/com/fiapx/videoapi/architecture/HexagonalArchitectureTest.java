package br.com.fiapx.videoapi.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HexagonalArchitectureTest {

    private static final String ROOT = "br.com.fiapx.videoapi";
    private static final String FIXTURES = ROOT + ".architecture.fixture";

    private final ClassFileImporter importer = new ClassFileImporter();

    @Test
    void productionPackagesAreExplicitlyAllowlisted() {
        var production = importer.importPath(Path.of("target/classes"));

        assertThat(production).allMatch(javaClass -> isAllowedPackage(javaClass.getPackageName()));
    }

    @Test
    void productionCodeRespectsHexagonalBoundaries() {
        var production = importer.importPath(Path.of("target/classes"));

        rules().forEach(rule -> rule.check(production));
    }

    @Test
    void rulesRejectRepresentativeNegativeFixtures() {
        var fixtures = importer.importPackages(FIXTURES);

        rules().forEach(rule -> assertThat(rule.evaluate(fixtures).hasViolation())
            .as(rule.getDescription())
            .isTrue());
    }

    private java.util.List<ArchRule> rules() {
        return java.util.List.of(
            ArchitectureRules.coreIsIndependent(),
            ArchitectureRules.technicalBucketsAreForbidden(),
            ArchitectureRules.coreDoesNotDependOnAdapters(),
            ArchitectureRules.dependenciesAreImmutable(),
            ArchitectureRules.fieldInjectionIsForbidden(),
            ArchitectureRules.mediaAndNotificationDependenciesAreForbidden()
        );
    }

    private boolean isAllowedPackage(String packageName) {
        return packageName.equals(ROOT)
            || packageName.startsWith(ROOT + ".foundation.")
            || packageName.matches(
                ROOT + "\\.(identity|videos|jobs|outbox|inbox)\\.(domain|application|adapter)(\\..+)?"
            );
    }
}
