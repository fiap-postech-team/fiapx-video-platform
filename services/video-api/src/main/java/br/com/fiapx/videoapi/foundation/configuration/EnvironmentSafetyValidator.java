package br.com.fiapx.videoapi.foundation.configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Rejects missing or development-only database settings before the context is created.
 */
public final class EnvironmentSafetyValidator implements EnvironmentPostProcessor {

    /**
     * Creates the stateless environment validator loaded by Spring Boot.
     */
    public EnvironmentSafetyValidator() {
    }

    @Override
    public void postProcessEnvironment(
        ConfigurableEnvironment environment,
        SpringApplication application
    ) {
        validate(environment, bindProperties(environment));
    }

    /**
     * Validates the effective datasource configuration without exposing its values.
     *
     * @param currentEnvironment active Spring environment
     * @param properties effective datasource properties
     */
    public void validate(Environment currentEnvironment, DataSourceProperties properties) {
        if (currentEnvironment.matchesProfiles("local")) {
            return;
        }
        requireValue("spring.datasource.url", properties.getUrl());
        requireValue("spring.datasource.username", properties.getUsername());
        requireValue("spring.datasource.password", properties.getPassword());
        rejectLocalDefaults(properties);
    }

    private DataSourceProperties bindProperties(ConfigurableEnvironment environment) {
        requireResolvedValue(environment, "spring.datasource.url");
        requireResolvedValue(environment, "spring.datasource.username");
        requireResolvedValue(environment, "spring.datasource.password");
        return Binder.get(environment)
            .bind("spring.datasource", DataSourceProperties.class)
            .orElseThrow(() -> missing("spring.datasource.url"));
    }

    private void requireResolvedValue(Environment environment, String propertyName) {
        try {
            requireValue(propertyName, environment.getProperty(propertyName));
        } catch (IllegalArgumentException exception) {
            throw missing(propertyName);
        }
    }

    private void requireValue(String propertyName, String value) {
        if (!StringUtils.hasText(value)) {
            throw missing(propertyName);
        }
    }

    private IllegalStateException missing(String propertyName) {
        return new IllegalStateException("Required configuration is missing: " + propertyName);
    }

    private void rejectLocalDefaults(DataSourceProperties properties) {
        rejectLocalDefault(
            "spring.datasource.url",
            properties.getUrl(),
            "jdbc:postgresql://localhost:5432/fiapx"
        );
        rejectLocalDefault("spring.datasource.username", properties.getUsername(), "fiapx");
        rejectLocalDefault("spring.datasource.password", properties.getPassword(), "fiapx");
    }

    private void rejectLocalDefault(String propertyName, String value, String localDefault) {
        if (localDefault.equals(value)) {
            throw new IllegalStateException(
                "Local default is not allowed outside the local profile: " + propertyName
            );
        }
    }
}
