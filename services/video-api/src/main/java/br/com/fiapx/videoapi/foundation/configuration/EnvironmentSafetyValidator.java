package br.com.fiapx.videoapi.foundation.configuration;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public final class EnvironmentSafetyValidator implements InitializingBean {

    private final Environment environment;
    private final DataSourceProperties dataSourceProperties;

    public EnvironmentSafetyValidator(
        Environment environment,
        DataSourceProperties dataSourceProperties
    ) {
        this.environment = environment;
        this.dataSourceProperties = dataSourceProperties;
    }

    @Override
    public void afterPropertiesSet() {
        validate(environment, dataSourceProperties);
    }

    public void validate(Environment currentEnvironment, DataSourceProperties properties) {
        if (currentEnvironment.matchesProfiles("local")) {
            return;
        }
        requireValue("spring.datasource.url", properties.getUrl());
        requireValue("spring.datasource.username", properties.getUsername());
        requireValue("spring.datasource.password", properties.getPassword());
        rejectLocalDefaults(properties);
    }

    private void requireValue(String propertyName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Required configuration is missing: " + propertyName);
        }
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
