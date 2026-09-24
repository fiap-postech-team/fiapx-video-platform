package br.com.fiapx.videoprocessor.processing.infrastructure.messaging.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import br.com.fiapx.videoprocessor.processing.application.port.out.JobEventPublisher;
import br.com.fiapx.videoprocessor.processing.domain.JobEvent;
import br.com.fiapx.videoprocessor.processing.domain.JobEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

@ExtendWith(MockitoExtension.class)
class FailedEventMessageRecovererTest {

    private static final Instant NOW = Instant.parse("2026-08-30T20:00:00Z");

    @Mock
    private JobEventPublisher publisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void announcesATerminalFailureBeforeDeadLetteringTheMessage() {
        UUID jobId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        Message message = requestFor(jobId, correlationId);

        assertThatThrownBy(() -> recoverer().recover(message, new IllegalStateException("connection reset")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        ArgumentCaptor<JobEvent> captor = ArgumentCaptor.forClass(JobEvent.class);
        verify(publisher).publish(captor.capture());
        JobEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(JobEventType.FAILED);
        assertThat(event.jobId()).isEqualTo(jobId);
        assertThat(event.correlationId()).isEqualTo(correlationId);
        assertThat(event.terminal()).isTrue();
        assertThat(event.occurredAt()).isEqualTo(NOW);
    }

    @Test
    void keepsTheCauseOutOfThePublishedReason() {
        Message message = requestFor(UUID.randomUUID(), null);
        Throwable cause = new IllegalStateException("ffmpeg failed for s3://videos/uploads/secret.mp4");

        assertThatThrownBy(() -> recoverer().recover(message, cause))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        ArgumentCaptor<JobEvent> captor = ArgumentCaptor.forClass(JobEvent.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().reason())
                .isEqualTo(FailedEventMessageRecoverer.REASON)
                .doesNotContain("ffmpeg", "s3://", "secret.mp4");
    }

    @Test
    void deadLettersAnUnreadablePayloadWithoutPublishingAnything() {
        Message message = new Message("not json".getBytes(StandardCharsets.UTF_8), new MessageProperties());

        assertThatThrownBy(() -> recoverer().recover(message, new IllegalStateException("boom")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(publisher);
    }

    private FailedEventMessageRecoverer recoverer() {
        return new FailedEventMessageRecoverer(publisher, objectMapper, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private Message requestFor(UUID jobId, UUID correlationId) {
        String json =
                """
                {"eventId":"%s","jobId":"%s","userId":"%s","sourceKey":"uploads/video.mp4","correlationId":%s}
                """
                        .formatted(
                                UUID.randomUUID(),
                                jobId,
                                UUID.randomUUID(),
                                correlationId == null ? "null" : "\"" + correlationId + "\"");
        return new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }
}
