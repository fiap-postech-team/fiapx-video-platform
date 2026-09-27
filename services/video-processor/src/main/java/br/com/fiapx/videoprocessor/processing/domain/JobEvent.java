package br.com.fiapx.videoprocessor.processing.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Integration event describing what happened to a job. Consumers deduplicate by {@code eventId} and
 * correlate by {@code correlationId}.
 */
public record JobEvent(
        UUID eventId,
        UUID jobId,
        JobEventType type,
        Instant occurredAt,
        UUID correlationId,
        ResultLocation resultLocation,
        Boolean terminal,
        String reason,
        String recipient,
        String videoName) {

    public JobEvent {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(jobId, "jobId is required");
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
        Objects.requireNonNull(correlationId, "correlationId is required");
    }

    public static JobEvent processing(VideoJob job, Instant occurredAt) {
        return new JobEvent(
                UUID.randomUUID(), job.jobId(), JobEventType.PROCESSING, occurredAt, job.correlationId(),
                null, null, null, null, null);
    }

    public static JobEvent completed(VideoJob job, ResultLocation resultLocation, Instant occurredAt) {
        Objects.requireNonNull(resultLocation, "resultLocation is required for a completed event");
        return new JobEvent(
                UUID.randomUUID(),
                job.jobId(),
                JobEventType.COMPLETED,
                occurredAt,
                job.correlationId(),
                resultLocation,
                null,
                null,
                job.recipient(),
                job.videoName());
    }

    public static JobEvent failed(UUID jobId, UUID correlationId, String reason, boolean terminal, Instant occurredAt) {
        return failed(jobId, correlationId, reason, terminal, occurredAt, null, null);
    }

    public static JobEvent failed(UUID jobId, UUID correlationId, String reason, boolean terminal, Instant occurredAt,
                                  String recipient, String videoName) {
        return new JobEvent(
                UUID.randomUUID(),
                jobId,
                JobEventType.FAILED,
                occurredAt,
                correlationId == null ? jobId : correlationId,
                null,
                terminal,
                reason,
                recipient,
                videoName);
    }

    public String resultKey() {
        return resultLocation == null ? null : resultLocation.key();
    }
}
