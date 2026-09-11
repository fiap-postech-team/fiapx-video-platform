package br.com.fiapx.notification.infrastructure.messaging.rabbit;

import br.com.fiapx.notification.domain.model.FailureNotification;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FailureEventMapper {

    public static FailureNotification toDomain(FailureEventMessage message, String defaultRecipient) {
        Objects.requireNonNull(message, "message is required");

        String recipient = normalize(message.getRecipient());
        if (recipient == null) {
            recipient = defaultRecipient;
        }

        return new FailureNotification(
                message.getEventId(),
                message.getJobId(),
                recipient,
                normalize(message.getReason())
        );
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
