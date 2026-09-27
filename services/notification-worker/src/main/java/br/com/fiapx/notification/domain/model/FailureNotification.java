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

    private static final int MAX_VIDEO_NAME_LENGTH = 512;

    private final UUID eventId;
    private final UUID jobId;
    private final String recipient;
    private final String reason;
    private final String videoName;
    private final ProcessingOutcome outcome;

    public FailureNotification(UUID eventId, UUID jobId, String recipient, String reason) {
        this(eventId, jobId, recipient, reason, null, ProcessingOutcome.FAILED);
    }

    public FailureNotification(UUID eventId, UUID jobId, String recipient, String reason,
                               String videoName, ProcessingOutcome outcome) {
        this.eventId = Objects.requireNonNull(eventId, "eventId is required");
        this.jobId = Objects.requireNonNull(jobId, "jobId is required");
        this.recipient = requireRecipient(recipient);
        this.reason = normalizeReason(reason);
        this.videoName = normalizeVideoName(videoName);
        this.outcome = Objects.requireNonNull(outcome, "outcome is required");
    }

    private static String normalizeVideoName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\p{Cntrl}", "").trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() <= MAX_VIDEO_NAME_LENGTH
                ? normalized
                : normalized.substring(0, MAX_VIDEO_NAME_LENGTH);
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
