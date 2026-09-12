package br.com.fiapx.notification.infrastructure.messaging.rabbit;

import br.com.fiapx.notification.domain.model.FailureNotification;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FailureEventMapperTest {

    @Test
    void shouldUseConfiguredFallbackWhenRecipientIsMissing() {
        UUID eventId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        FailureEventMessage message = new FailureEventMessage(eventId, jobId, null, null);

        FailureNotification failure = FailureEventMapper.toDomain(message, "dev@fiapx.local");

        assertEquals(eventId, failure.getEventId());
        assertEquals(jobId, failure.getJobId());
        assertEquals("dev@fiapx.local", failure.getRecipient());
        assertEquals("erro não informado", failure.getReason());
    }

    @Test
    void shouldUseConfiguredFallbackWhenRecipientIsBlank() {
        UUID eventId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        FailureEventMessage message = new FailureEventMessage(eventId, jobId, "   ", "failure");

        FailureNotification failure = FailureEventMapper.toDomain(message, "dev@fiapx.local");

        assertEquals("dev@fiapx.local", failure.getRecipient());
    }

    @Test
    void shouldRejectMessageWithoutEventId() {
        FailureEventMessage message = new FailureEventMessage(null, UUID.randomUUID(), null, "failure");

        assertThrows(
                NullPointerException.class,
                () -> FailureEventMapper.toDomain(message, "dev@fiapx.local")
        );
    }
}
