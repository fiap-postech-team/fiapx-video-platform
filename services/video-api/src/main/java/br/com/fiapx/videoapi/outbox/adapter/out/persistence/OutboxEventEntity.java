package br.com.fiapx.videoapi.outbox.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {
    @Id private UUID id;
    private String routingKey;
    private String payload;
    private Instant createdAt;
    protected OutboxEventEntity() { }
    OutboxEventEntity(UUID id, String payload) { this.id = id; routingKey = "video.job.requested.v1"; this.payload = payload; createdAt = Instant.now(); }
}
