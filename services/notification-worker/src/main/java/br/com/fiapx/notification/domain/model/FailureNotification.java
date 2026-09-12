package br.com.fiapx.notification.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

@Getter
@EqualsAndHashCode
public class FailureNotification {

    private static final int MAX_REASON_LENGTH = 500;
    private static final String DEFAULT_REASON = "erro não informado";

    private final UUID eventId;
    private final UUID jobId;
    private final String recipient;
    private final String reason;

    public FailureNotification(UUID eventId, UUID jobId, String recipient, String reason) {
        this.eventId = Objects.requireNonNull(eventId, "eventId is required");
        this.jobId = Objects.requireNonNull(jobId, "jobId is required");
        this.recipient = requireRecipient(recipient);
        this.reason = normalizeReason(reason);
    }

    private static String normalizeReason(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_REASON;
        }

        String normalized = value.trim();
        return normalized.length() <= MAX_REASON_LENGTH
                ? normalized
                : normalized.substring(0, MAX_REASON_LENGTH);
    }

    private static String requireRecipient(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("recipient is required");
        }

        String normalized = value.trim();
        if (normalized.length() > 320) {
            throw new IllegalArgumentException("recipient must have at most 320 characters");
        }
        return normalized;
    }
}
