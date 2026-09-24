package br.com.fiapx.videoprocessor.processing.infrastructure.messaging.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import br.com.fiapx.videoprocessor.processing.domain.JobEvent;
import br.com.fiapx.videoprocessor.processing.domain.JobEventType;
import br.com.fiapx.videoprocessor.processing.domain.ResultLocation;
import br.com.fiapx.videoprocessor.processing.domain.VideoJob;
import br.com.fiapx.videoprocessor.processing.infrastructure.messaging.MessagingProperties;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class RabbitJobEventPublisherTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-08-30T20:00:00Z");
    private static final MessagingProperties PROPERTIES = new MessagingProperties(
            "video.events",
            "video.processing.v1",
            "video.events.dlx",
            "video.processing.dlq.v1",
            new MessagingProperties.RoutingKeys(
                    "video.job.requested.v1",
                    "video.job.started.v1",
                    "video.job.completed.v1",
                    "video.job.failed.v1"));

    @Mock
    private RabbitTemplate rabbitTemplate;

    @ParameterizedTest
    @CsvSource({
        "PROCESSING, video.job.started.v1",
        "COMPLETED, video.job.completed.v1",
        "FAILED, video.job.failed.v1"
    })
    void routesEachEventTypeToTheKeyDeclaredInTheContract(JobEventType type, String expectedRoutingKey) {
        new RabbitJobEventPublisher(rabbitTemplate, PROPERTIES).publish(eventOfType(type));

        verify(rabbitTemplate).convertAndSend(eq("video.events"), eq(expectedRoutingKey), any(Object.class), any(MessagePostProcessor.class));
    }

    @Test
    void publishesTheCompletionPayloadDescribedByTheContract() {
        VideoJob job = new VideoJob(UUID.randomUUID(), UUID.randomUUID(), "uploads/video.mp4");
        ResultLocation resultLocation = job.resultLocation();
        JobEvent event = JobEvent.completed(job, resultLocation, OCCURRED_AT);

        new RabbitJobEventPublisher(rabbitTemplate, PROPERTIES).publish(event);

        JobResultMessage payload = capturedPayload();
        assertThat(payload.eventId()).isEqualTo(event.eventId());
        assertThat(payload.jobId()).isEqualTo(job.jobId());
        assertThat(payload.type()).isEqualTo("COMPLETED");
        assertThat(payload.schemaVersion()).isEqualTo("1.0");
        assertThat(payload.occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(payload.correlationId()).isEqualTo(job.correlationId());
        assertThat(payload.resultKey()).isEqualTo(resultLocation.key());
        assertThat(payload.terminal()).isNull();
        assertThat(payload.reason()).isNull();
    }

    @Test
    void publishesTheTerminalFailurePayloadDescribedByTheContract() {
        UUID jobId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        JobEvent event = JobEvent.failed(jobId, correlationId, "The submitted video could not be decoded", true, OCCURRED_AT);

        new RabbitJobEventPublisher(rabbitTemplate, PROPERTIES).publish(event);

        JobResultMessage payload = capturedPayload();
        assertThat(payload.type()).isEqualTo("FAILED");
        assertThat(payload.terminal()).isTrue();
        assertThat(payload.reason()).isEqualTo("The submitted video could not be decoded");
        assertThat(payload.resultKey()).isNull();
        assertThat(payload.correlationId()).isEqualTo(correlationId);
    }

    @Test
    void carriesTheEventAndCorrelationIdsAsBrokerMetadata() {
        JobEvent event = eventOfType(JobEventType.PROCESSING);

        new RabbitJobEventPublisher(rabbitTemplate, PROPERTIES).publish(event);

        Message message = capturedPostProcessor().postProcessMessage(new Message(new byte[0], new MessageProperties()));
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(event.eventId().toString());
        assertThat(message.getMessageProperties().getCorrelationId()).isEqualTo(event.correlationId().toString());
    }

    private JobResultMessage capturedPayload() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), captor.capture(), any(MessagePostProcessor.class));
        return (JobResultMessage) captor.getValue();
    }

    private MessagePostProcessor capturedPostProcessor() {
        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class), captor.capture());
        return captor.getValue();
    }

    private static JobEvent eventOfType(JobEventType type) {
        VideoJob job = new VideoJob(UUID.randomUUID(), UUID.randomUUID(), "uploads/video.mp4");
        return switch (type) {
            case PROCESSING -> JobEvent.processing(job, OCCURRED_AT);
            case COMPLETED -> JobEvent.completed(job, job.resultLocation(), OCCURRED_AT);
            case FAILED -> JobEvent.failed(job.jobId(), job.correlationId(), "failed", true, OCCURRED_AT);
        };
    }
}
