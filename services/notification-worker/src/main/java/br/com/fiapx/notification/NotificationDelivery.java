package br.com.fiapx.notification;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_deliveries", uniqueConstraints = @UniqueConstraint(columnNames = "eventId"))
public class NotificationDelivery {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID eventId;
    @Column(nullable = false)
    private UUID jobId;
    @Column(nullable = false)
    private String recipient;
    @Column(nullable = false)
    private Instant deliveredAt;

    protected NotificationDelivery() {
    }

    public NotificationDelivery(UUID eventId, UUID jobId, String recipient) {
        id = UUID.randomUUID();
        this.eventId = eventId;
        this.jobId = jobId;
        this.recipient = recipient;
        deliveredAt = Instant.now();
    }
}
