package br.com.fiapx.api.job;

import java.time.Instant;
import java.util.UUID;

/** Operational metadata exposed to ADMIN queries. Read-only; carries no credentials, hashes or tokens. */
public record AdminJobResponse(UUID id, UUID userId, String sourceKey, String resultKey, Job.Status status, Instant createdAt, Instant updatedAt) {
    static AdminJobResponse from(Job job) {
        return new AdminJobResponse(job.getId(), job.getUserId(), job.getSourceKey(), job.getResultKey(), job.getStatus(), job.getCreatedAt(), job.getUpdatedAt());
    }
}
