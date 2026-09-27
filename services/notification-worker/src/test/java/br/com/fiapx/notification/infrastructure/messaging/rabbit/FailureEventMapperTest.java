package br.com.fiapx.notification.infrastructure.messaging.rabbit;

import br.com.fiapx.notification.domain.model.FailureNotification;
import br.com.fiapx.notification.domain.model.ProcessingOutcome;
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
        assertEquals(ProcessingOutcome.FAILED, failure.getOutcome());
        assertEquals(null, failure.getVideoName());
    }

    @Test
    void shouldKeepTheVideoNameAndCompletedOutcome() {
        FailureEventMessage message = new FailureEventMessage(UUID.randomUUID(), UUID.randomUUID(), "person@example.test", null);
        message.setType("COMPLETED");
        message.setVideoName(" aula.mp4 ");

        FailureNotification failure = FailureEventMapper.toDomain(message, "dev@fiapx.local");

        assertEquals(ProcessingOutcome.COMPLETED, failure.getOutcome());
        assertEquals("aula.mp4", failure.getVideoName());
        assertEquals("person@example.test", failure.getRecipient());
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
