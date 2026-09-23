package br.com.fiapx.videoprocessor.processing.infrastructure.messaging;

import br.com.fiapx.videoprocessor.processing.domain.JobEventType;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RabbitMQ topology described by {@code contracts/asyncapi.yaml} and the event catalog.
 */
@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(
        String exchange, String queue, String deadLetterExchange, String deadLetterQueue, RoutingKeys routingKeys) {

    public record RoutingKeys(String requested, String started, String completed, String failed) {

        public String forEventType(JobEventType type) {
            return switch (type) {
                case PROCESSING -> started;
                case COMPLETED -> completed;
                case FAILED -> failed;
            };
        }
    }
}
