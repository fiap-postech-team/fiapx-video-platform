package br.com.fiapx.api.job;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "source_key", nullable = false, length = 1024)
    private String sourceKey;

    @Column(name = "result_key", length = 1024)
    private String resultKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Job() {
    }

    public Job(UUID userId, String sourceKey) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.sourceKey = sourceKey;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public String getResultKey() {
        return resultKey;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void apply(Status newStatus, String resultKey) {
        this.status = newStatus;
        this.resultKey = resultKey;
        this.updatedAt = Instant.now();
    }

    public enum Status {
        PENDING,
        STARTED,
        COMPLETED,
        FAILED
    }
}
