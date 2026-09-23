package br.com.fiapx.notification.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "notification_deliveries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_deliveries_event_id",
                columnNames = "event_id"
        )
)
public class NotificationDeliveryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "job_id", nullable = false, updatable = false)
    private UUID jobId;

    @Column(name = "recipient", nullable = false, length = 320, updatable = false)
    private String recipient;

    @Column(name = "delivered_at", nullable = false, updatable = false)
    private Instant deliveredAt;

    public NotificationDeliveryEntity(UUID eventId, UUID jobId, String recipient, Instant deliveredAt) {
        this.eventId = eventId;
        this.jobId = jobId;
        this.recipient = recipient;
        this.deliveredAt = deliveredAt;
    }
}
