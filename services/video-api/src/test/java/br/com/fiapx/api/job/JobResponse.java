package br.com.fiapx.api.job;

import java.time.Instant;
import java.util.UUID;

/** Public HTTP contract for job queries; never exposes the JPA entity. */
public record JobResponse(UUID id, UUID userId, String sourceKey, String resultKey, Job.Status status, Instant createdAt) {
    static JobResponse from(Job job) {
        return new JobResponse(job.getId(), job.getUserId(), job.getSourceKey(), job.getResultKey(), job.getStatus(), job.getCreatedAt());
    }
}
