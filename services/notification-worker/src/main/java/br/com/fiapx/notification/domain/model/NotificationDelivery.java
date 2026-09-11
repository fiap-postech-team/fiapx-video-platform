package br.com.fiapx.notification.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@EqualsAndHashCode
public class NotificationDelivery {

    private final UUID eventId;
    private final UUID jobId;
    private final String recipient;
    private final Instant deliveredAt;

    public NotificationDelivery(UUID eventId, UUID jobId, String recipient, Instant deliveredAt) {
        this.eventId = Objects.requireNonNull(eventId, "eventId is required");
        this.jobId = Objects.requireNonNull(jobId, "jobId is required");
        this.recipient = Objects.requireNonNull(recipient, "recipient is required");
        this.deliveredAt = Objects.requireNonNull(deliveredAt, "deliveredAt is required");
    }
}
