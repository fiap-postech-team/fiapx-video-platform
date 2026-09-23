package br.com.fiapx.videoapi.inbox.domain;

import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import java.time.Instant;
import java.util.UUID;

public record JobResultEvent(UUID eventId, UUID jobId, JobStatus status, String resultKey,
                             String reasonCode, Instant occurredAt, String fingerprint) {
}
