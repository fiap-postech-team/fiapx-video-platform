package br.com.fiapx.videoapi.outbox.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {
    @Id private UUID id;
    private String routingKey;
    private String payload;
    private Instant createdAt;
    private Instant publishedAt;
    private UUID aggregateId;
    private String aggregateType;
    private String eventType;
    private int schemaVersion;
    private UUID correlationId;
    @Enumerated(EnumType.STRING) private OutboxStatus status;
    private int attempts;
    private Instant nextAttemptAt;
    private String claimedBy;
    private UUID claimToken;
    private Instant claimedAt;
    private Instant claimExpiresAt;
    private String lastErrorCode;
    protected OutboxEventEntity() { }
    OutboxEventEntity(UUID id, UUID jobId, String payload, Instant now) {
        this.id = id; aggregateId = jobId; aggregateType = "JOB"; routingKey = "video.job.requested.v1";
        eventType = routingKey; schemaVersion = 1; correlationId = jobId; this.payload = payload;
        createdAt = now; nextAttemptAt = now; status = OutboxStatus.PENDING;
    }

    boolean isClaimedBy(UUID token) { return claimToken != null && claimToken.equals(token); }
    OutboxClaimView claim(UUID token, String instance, Instant now, Instant expiresAt) {
        status = OutboxStatus.PROCESSING; claimToken = token; claimedBy = instance; claimedAt = now; claimExpiresAt = expiresAt;
        return new OutboxClaimView(id, token, routingKey, payload, createdAt);
    }
    void publish(Instant now) { status = OutboxStatus.PUBLISHED; publishedAt = now; clearClaim(); }
    void retry(Instant next, String error) { status = OutboxStatus.PENDING; attempts++; nextAttemptAt = next; lastErrorCode = error; clearClaim(); }
    private void clearClaim() { claimToken = null; claimedBy = null; claimedAt = null; claimExpiresAt = null; }

    record OutboxClaimView(UUID id, UUID token, String routingKey, String payload, Instant occurredAt) { }
}
