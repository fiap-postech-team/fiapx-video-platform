package br.com.fiapx.notification.infrastructure.messaging.rabbit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FailureEventMessage {

    @NotNull(message = "eventId is required")
    private UUID eventId;

    @NotNull(message = "jobId is required")
    private UUID jobId;

    @Email(message = "recipient must be a valid email address")
    @Size(max = 320, message = "recipient must have at most 320 characters")
    private String recipient;

    @Size(max = 500, message = "reason must have at most 500 characters")
    private String reason;

    public FailureEventMessage(UUID eventId, UUID jobId, String recipient, String reason) {
        this.eventId = eventId;
        this.jobId = jobId;
        this.recipient = normalizeOptionalText(recipient);
        this.reason = normalizeOptionalText(reason);
    }

    public void setRecipient(String recipient) {
        this.recipient = normalizeOptionalText(recipient);
    }

    public void setReason(String reason) {
        this.reason = normalizeOptionalText(reason);
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
