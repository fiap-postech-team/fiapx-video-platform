package br.com.fiapx.videoprocessor.processing.infrastructure.messaging.out;

import br.com.fiapx.videoprocessor.processing.domain.JobEvent;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

/**
 * Wire shape of {@code JobResult} in {@code contracts/asyncapi.yaml}.
 *
 * <p>{@code recipient} and {@code videoName} are copied from the request when the producer sent them.
 * The notification worker uses them to address the user without reading the API database.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobResultMessage(
        UUID eventId,
        UUID jobId,
        String type,
        String schemaVersion,
        Instant occurredAt,
        UUID correlationId,
        String resultKey,
        Boolean terminal,
        String reason,
        String recipient,
        String videoName) {

    static final String SCHEMA_VERSION = "1.0";

    public static JobResultMessage from(JobEvent event) {
        return new JobResultMessage(
                event.eventId(),
                event.jobId(),
                event.type().name(),
                SCHEMA_VERSION,
                event.occurredAt(),
                event.correlationId(),
                event.resultKey(),
                event.terminal(),
                event.reason(),
                event.recipient(),
                event.videoName());
    }
}
