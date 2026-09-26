package br.com.fiapx.videoapi.outbox.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {
    @Id private UUID id;
    private String routingKey;
    @Column(nullable = false)
    private String payload;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", columnDefinition = "jsonb")
    private Map<String, Object> payloadJson;
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
    OutboxEventEntity(UUID id, UUID jobId, String payload, Map<String, Object> payloadJson,
                      Instant occurredAt) {
        this(id, jobId, payload, payloadJson, occurredAt, jobId);
    }
    OutboxEventEntity(UUID id, UUID jobId, String payload, Map<String, Object> payloadJson,
                      Instant occurredAt, UUID correlationId) {
        this.id = id; aggregateId = jobId; aggregateType = "JOB"; routingKey = "video.job.requested.v1";
        eventType = routingKey; schemaVersion = 1; this.correlationId = correlationId; this.payload = payload;
        this.payloadJson = payloadJson; createdAt = occurredAt; nextAttemptAt = occurredAt;
        status = OutboxStatus.PENDING;
    }

    OutboxClaimView claim(UUID token, String instance, Instant now, Instant expiresAt) {
        boolean recovered = status == OutboxStatus.PROCESSING;
        status = OutboxStatus.PROCESSING; attempts++; claimToken = token; claimedBy = instance;
        claimedAt = now; claimExpiresAt = expiresAt;
        return new OutboxClaimView(id, aggregateId, correlationId, token, routingKey, payload,
            createdAt, attempts, recovered);
    }

    record OutboxClaimView(UUID id, UUID jobId, UUID correlationId, UUID token, String routingKey,
                           String payload, Instant occurredAt, int attempts, boolean recovered) { }
}
