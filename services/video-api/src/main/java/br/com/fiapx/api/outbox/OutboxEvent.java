package br.com.fiapx.api.outbox;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String routingKey;
    @Column(nullable = false, columnDefinition = "text")
    private String payload;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(String routingKey, String payload) {
        id = UUID.randomUUID();
        this.routingKey = routingKey;
        this.payload = payload;
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getPayload() {
        return payload;
    }

    public boolean unpublished() {
        return publishedAt == null;
    }

    public void published() {
        publishedAt = Instant.now();
    }
}
