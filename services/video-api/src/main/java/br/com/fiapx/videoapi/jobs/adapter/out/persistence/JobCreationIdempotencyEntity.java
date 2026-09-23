package br.com.fiapx.videoapi.jobs.adapter.out.persistence;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_creation_idempotency")
class JobCreationIdempotencyEntity {
    @EmbeddedId
    private JobCreationIdempotencyId id;
    private String requestFingerprint;
    private UUID jobId;
    private Instant createdAt;

    protected JobCreationIdempotencyEntity() {
    }

    JobCreationIdempotencyEntity(UUID userId, String key, String fingerprint, UUID jobId, Instant createdAt) {
        id = new JobCreationIdempotencyId(userId, key);
        requestFingerprint = fingerprint;
        this.jobId = jobId;
        this.createdAt = createdAt;
    }

    String requestFingerprint() {
        return requestFingerprint;
    }

    UUID jobId() {
        return jobId;
    }
}
