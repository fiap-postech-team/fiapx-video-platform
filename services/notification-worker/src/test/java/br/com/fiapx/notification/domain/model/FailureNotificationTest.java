package br.com.fiapx.notification.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FailureNotificationTest {

    @Test
    void shouldUseSafeDefaultReasonWhenReasonIsBlank() {
        FailureNotification failure = new FailureNotification(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user@example.com",
                "   "
        );

        assertEquals("erro não informado", failure.getReason());
    }

    @Test
    void shouldLimitReasonToFiveHundredCharacters() {
        FailureNotification failure = new FailureNotification(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user@example.com",
                "x".repeat(600)
        );

        assertEquals(500, failure.getReason().length());
    }

    @Test
    void shouldRejectRecipientLargerThanDatabaseColumn() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FailureNotification(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "x".repeat(321),
                        "failure"
                )
        );
    }
}
