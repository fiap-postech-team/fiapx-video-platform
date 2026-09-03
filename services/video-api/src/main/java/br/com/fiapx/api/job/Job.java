package br.com.fiapx.api.job;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private String sourceKey;
    private String resultKey;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant updatedAt;

    protected Job() {
    }

    public Job(UUID userId, String sourceKey) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.sourceKey = sourceKey;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
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

    public void apply(Status status, String resultKey) {
        this.status = status;
        this.resultKey = resultKey;
        this.updatedAt = Instant.now();
    }

    public enum Status {PENDING, PROCESSING, COMPLETED, FAILED}
}
