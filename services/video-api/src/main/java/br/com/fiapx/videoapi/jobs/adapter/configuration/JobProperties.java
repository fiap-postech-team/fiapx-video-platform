package br.com.fiapx.videoapi.jobs.adapter.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jobs")
public record JobProperties(int maxPageSize, boolean resultListenerEnabled) {
    public JobProperties {
        if (maxPageSize < 1) {
            throw new IllegalArgumentException("Job page size must be positive");
        }
    }

}
