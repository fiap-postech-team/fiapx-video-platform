package br.com.fiapx.videoapi.inbox.domain;

import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import java.time.Instant;
import java.util.UUID;

public record JobResultEvent(UUID eventId, UUID jobId, JobStatus status, String resultKey,
                             String reasonCode, Instant occurredAt, String fingerprint,
                             String routingKey, int schemaVersion, UUID correlationId) {
    public JobResultEvent(UUID eventId, UUID jobId, JobStatus status, String resultKey,
                           String reasonCode, Instant occurredAt, String fingerprint) {
        this(eventId, jobId, status, resultKey, reasonCode, occurredAt, fingerprint,
            "video.job." + (status == JobStatus.PROCESSING ? "started" : status.name().toLowerCase()) + ".v1", 1, jobId);
    }
}
