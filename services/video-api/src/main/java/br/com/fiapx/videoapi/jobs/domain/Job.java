package br.com.fiapx.videoapi.jobs.domain;

import java.time.Instant;
import java.util.UUID;

public final class Job {
    private final UUID id;
    private final UUID userId;
    private final UUID videoId;
    private final JobSourceKind sourceKind;
    private final String sourceKey;
    private final Instant createdAt;
    private final boolean libraryVisible;
    private JobStatus status;
    private String resultKey;

    public Job(UUID id, UUID userId, String sourceKey, Instant createdAt) {
        this(id, userId, null, JobSourceKind.LEGACY_KEY, sourceKey, null, JobStatus.PENDING, createdAt);
    }

    public Job(UUID id, UUID userId, String sourceKey, String resultKey, JobStatus status, Instant createdAt) {
        this(id, userId, null, JobSourceKind.LEGACY_KEY, sourceKey, resultKey, status, createdAt);
    }

    public Job(UUID id, UUID userId, UUID videoId, String sourceKey, Instant createdAt) {
        this(id, userId, videoId, JobSourceKind.VIDEO, sourceKey, null, JobStatus.PENDING, createdAt);
    }

    public Job(UUID id, UUID userId, UUID videoId, JobSourceKind sourceKind, String sourceKey,
               String resultKey, JobStatus status, Instant createdAt) {
        this(id, userId, videoId, sourceKind, sourceKey, resultKey, status, createdAt,
            sourceKind == JobSourceKind.VIDEO);
    }

    public Job(UUID id, UUID userId, UUID videoId, JobSourceKind sourceKind, String sourceKey,
               String resultKey, JobStatus status, Instant createdAt, boolean libraryVisible) {
        this.id = id; this.userId = userId; this.videoId = videoId; this.sourceKind = sourceKind;
        this.sourceKey = sourceKey; this.resultKey = resultKey; this.status = status;
        this.createdAt = createdAt; this.libraryVisible = libraryVisible;
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
    public UUID videoId() { return videoId; }
    public JobSourceKind sourceKind() { return sourceKind; }
    public String sourceKey() { return sourceKey; }
    public String resultKey() { return resultKey; }
    public JobStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public boolean libraryVisible() { return libraryVisible; }
}
