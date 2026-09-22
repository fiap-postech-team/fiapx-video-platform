package br.com.fiapx.videoapi.outbox.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiapx.videoapi.outbox.adapter.configuration.OutboxMessagingConfiguration;
import br.com.fiapx.videoapi.outbox.adapter.configuration.OutboxProperties;
import br.com.fiapx.videoapi.outbox.adapter.observability.OutboxMetrics;
import br.com.fiapx.videoapi.outbox.application.port.out.OutboxStore;
import br.com.fiapx.videoapi.outbox.domain.OutboxClaim;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class OutboxPublisherTest {
    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private final OutboxStore outbox = mock(OutboxStore.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final OutboxMetrics metrics = new OutboxMetrics(registry, mock(JdbcTemplate.class));
    private OutboxClaim claim;

    @BeforeEach
    void setup() {
        claim = new OutboxClaim(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "video.job.requested.v1", "{}", NOW, 1, false);
        when(outbox.claimReady(anyInt(), anyString(), any(), any())).thenReturn(List.of(claim));
    }

    @AfterEach
    void closeRegistry() {
        registry.close();
    }

    @Test
    void waitsForPositiveConfirmAndMarksTheEventPublished() throws Exception {
        var publishedMessage = new java.util.concurrent.atomic.AtomicReference<Message>();
        confirmWith(true, false, publishedMessage);
        when(outbox.markPublished(eq(claim.eventId()), eq(claim.claimToken()), any())).thenReturn(true);

        publisher(properties(10, Duration.ofSeconds(5))).publishBatch();

        verify(outbox).markPublished(eq(claim.eventId()), eq(claim.claimToken()), any());
        assertThat(publishedMessage.get().getMessageProperties().getDeliveryMode())
            .isEqualTo(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
        assertThat(publishedMessage.get().getMessageProperties().getMessageId()).isEqualTo(claim.eventId().toString());
        assertThat(publishedMessage.get().getMessageProperties().getCorrelationId()).isEqualTo(claim.correlationId().toString());
        assertThat(registry.get("outbox.published").counter().count()).isEqualTo(1);
    }

    @Test
    void schedulesAnExponentialRetryAfterNegativeConfirm() throws Exception {
        confirmWith(false, false, new java.util.concurrent.atomic.AtomicReference<>());

        publisher(properties(10, Duration.ofSeconds(5))).publishBatch();

        verify(outbox).scheduleRetry(claim.eventId(), claim.claimToken(), NOW.plusSeconds(2), "NEGATIVE_CONFIRM");
        assertThat(registry.get("outbox.confirms.negative").counter().count()).isEqualTo(1);
    }

    @Test
    void treatsAnUnroutableReturnAsRetryableFailure() throws Exception {
        confirmWith(true, true, new java.util.concurrent.atomic.AtomicReference<>());

        publisher(properties(10, Duration.ofSeconds(5))).publishBatch();

        verify(outbox).scheduleRetry(claim.eventId(), claim.claimToken(), NOW.plusSeconds(2), "UNROUTABLE");
    }

    @Test
    void schedulesRetryAfterConfirmTimeout() {
        doAnswer(invocation -> null).when(rabbit).convertAndSend(anyString(), anyString(), anyString(),
            any(org.springframework.amqp.core.MessagePostProcessor.class), any(CorrelationData.class));

        publisher(properties(10, Duration.ofMillis(5))).publishBatch();

        verify(outbox).scheduleRetry(claim.eventId(), claim.claimToken(), NOW.plusSeconds(2), "CONFIRM_TIMEOUT");
    }

    @Test
    void marksTheLastFailedAttemptForDiagnosis() throws Exception {
        claim = new OutboxClaim(claim.eventId(), claim.jobId(), claim.correlationId(), claim.claimToken(),
            claim.routingKey(), claim.payload(), claim.occurredAt(), 10, false);
        when(outbox.claimReady(anyInt(), anyString(), any(), any())).thenReturn(List.of(claim));
        confirmWith(false, false, new java.util.concurrent.atomic.AtomicReference<>());
        when(outbox.markFailed(claim.eventId(), claim.claimToken(), "NEGATIVE_CONFIRM")).thenReturn(true);

        publisher(properties(10, Duration.ofSeconds(5))).publishBatch();

        verify(outbox).markFailed(claim.eventId(), claim.claimToken(), "NEGATIVE_CONFIRM");
    }

    @Test
    void recordsExpiredClaimsWithoutAddingIdentifiersAsMetricTags() throws Exception {
        claim = new OutboxClaim(claim.eventId(), claim.jobId(), claim.correlationId(), claim.claimToken(),
            claim.routingKey(), claim.payload(), claim.occurredAt(), 1, true);
        when(outbox.claimReady(anyInt(), anyString(), any(), any())).thenReturn(List.of(claim));
        confirmWith(true, false, new java.util.concurrent.atomic.AtomicReference<>());
        when(outbox.markPublished(eq(claim.eventId()), eq(claim.claimToken()), any())).thenReturn(true);

        publisher(properties(10, Duration.ofSeconds(5))).publishBatch();

        assertThat(registry.get("outbox.claims.expired").counter().count()).isEqualTo(1);
        assertThat(registry.getMeters()).allSatisfy(meter ->
            assertThat(meter.getId().getTags()).noneMatch(tag -> tag.getValue().equals(claim.eventId().toString())));
    }

    private OutboxPublisher publisher(OutboxProperties properties) {
        return new OutboxPublisher(outbox, rabbit, properties, metrics, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private OutboxProperties properties(int maxAttempts, Duration confirmTimeout) {
        return new OutboxProperties(10, 1000, Duration.ofSeconds(30), confirmTimeout, maxAttempts, "api-test");
    }

    private void confirmWith(boolean ack, boolean returned,
                             java.util.concurrent.atomic.AtomicReference<Message> publishedMessage) throws Exception {
        doAnswer(invocation -> {
            var processor = invocation.getArgument(3, org.springframework.amqp.core.MessagePostProcessor.class);
            publishedMessage.set(processor.postProcessMessage(new Message("{}".getBytes(), new MessageProperties())));
            var correlation = invocation.getArgument(4, CorrelationData.class);
            if (returned) {
                correlation.setReturned(new ReturnedMessage(new Message(new byte[0]), 312, "NO_ROUTE",
                    OutboxMessagingConfiguration.EXCHANGE, claim.routingKey()));
            }
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, "test"));
            return null;
        }).when(rabbit).convertAndSend(anyString(), anyString(), anyString(),
            any(org.springframework.amqp.core.MessagePostProcessor.class), any(CorrelationData.class));
    }
}
