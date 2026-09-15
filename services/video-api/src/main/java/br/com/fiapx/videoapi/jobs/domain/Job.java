package br.com.fiapx.videoapi.jobs.domain;

import java.time.Instant;
import java.util.UUID;

public final class Job {
    private final UUID id;
    private final UUID userId;
    private final String sourceKey;
    private final Instant createdAt;
    private JobStatus status;
    private String resultKey;

    public Job(UUID id, UUID userId, String sourceKey, Instant createdAt) {
        this(id, userId, sourceKey, null, JobStatus.PENDING, createdAt);
    }

    public Job(UUID id, UUID userId, String sourceKey, String resultKey, JobStatus status, Instant createdAt) {
        this.id = id; this.userId = userId; this.sourceKey = sourceKey; this.resultKey = resultKey;
        this.status = status; this.createdAt = createdAt;
    }

    public void apply(JobStatus nextStatus, String nextResultKey) {
        if (!accepts(nextStatus)) throw new IllegalStateException("Invalid job transition");
        status = nextStatus;
        resultKey = nextResultKey;
    }

    private boolean accepts(JobStatus nextStatus) {
        return (status == JobStatus.PENDING && (nextStatus == JobStatus.PROCESSING || nextStatus == JobStatus.FAILED))
            || (status == JobStatus.PROCESSING && (nextStatus == JobStatus.COMPLETED || nextStatus == JobStatus.FAILED));
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public String sourceKey() { return sourceKey; }
    public String resultKey() { return resultKey; }
    public JobStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
}
