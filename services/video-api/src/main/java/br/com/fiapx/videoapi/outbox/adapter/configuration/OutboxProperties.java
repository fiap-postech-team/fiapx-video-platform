package br.com.fiapx.videoapi.outbox.adapter.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.outbox")
public record OutboxProperties(int batchSize, long pollIntervalMs, Duration claimTimeout,
                               Duration confirmTimeout, int maxAttempts, String instanceId) {
}
