package br.com.fiapx.videoapi.inbox.adapter.out.persistence;

import br.com.fiapx.videoapi.inbox.domain.JobResultEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inbox_events")
class InboxEventEntity {
    @Id UUID eventId;
    UUID jobId;
    String eventType;
    int schemaVersion;
    UUID correlationId;
    String payloadFingerprint;
    String status;
    Instant occurredAt;
    Instant receivedAt;
    Instant processedAt;

    protected InboxEventEntity() {
    }

    InboxEventEntity(JobResultEvent event) {
        eventId = event.eventId(); jobId = event.jobId(); eventType = event.status().name(); schemaVersion = 1;
        correlationId = event.jobId(); payloadFingerprint = event.fingerprint(); status = "PROCESSED";
        occurredAt = event.occurredAt(); receivedAt = Instant.now(); processedAt = receivedAt;
    }
}
