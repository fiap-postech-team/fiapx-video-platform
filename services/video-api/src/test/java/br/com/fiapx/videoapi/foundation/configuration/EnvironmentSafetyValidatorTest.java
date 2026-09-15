package br.com.fiapx.videoapi.foundation.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvironmentSafetyValidatorTest {

    private final EnvironmentSafetyValidator validator = new EnvironmentSafetyValidator();

    @Test
    void rejectsMissingDatabaseConfigurationOutsideLocalProfile() {
        assertThatThrownBy(() -> validator.validate(new MockEnvironment(), new DataSourceProperties()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("spring.datasource.url");
    }

    @Test
    void rejectsKnownLocalDefaultsOutsideLocalProfile() {
        assertThatThrownBy(() -> validator.validate(new MockEnvironment(), localProperties()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("spring.datasource.url")
            .hasMessageNotContaining("jdbc:postgresql");
    }

    @Test
    void acceptsKnownLocalDefaultsWithLocalProfile() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        assertThatCode(() -> validator.validate(environment, localProperties()))
            .doesNotThrowAnyException();
    }

    @Test
    void acceptsExternalDatabaseConfigurationOutsideLocalProfile() {
        var properties = new DataSourceProperties();
        properties.setUrl("jdbc:postgresql://database:5432/video");
        properties.setUsername("application");
        properties.setPassword("external-secret");

        assertThatCode(() -> validator.validate(new MockEnvironment(), properties))
            .doesNotThrowAnyException();
    }

    private DataSourceProperties localProperties() {
        var properties = new DataSourceProperties();
        properties.setUrl("jdbc:postgresql://localhost:5432/fiapx");
        properties.setUsername("fiapx");
        properties.setPassword("fiapx");
        return properties;
    }
}
