package br.com.fiapx.videoapi.outbox.adapter;

import br.com.fiapx.videoapi.outbox.adapter.configuration.OutboxMessagingConfiguration;
import br.com.fiapx.videoapi.outbox.adapter.configuration.OutboxProperties;
import br.com.fiapx.videoapi.outbox.adapter.observability.OutboxMetrics;
import br.com.fiapx.videoapi.outbox.application.port.out.OutboxStore;
import br.com.fiapx.videoapi.outbox.domain.OutboxClaim;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int MAX_BACKOFF_SECONDS = 300;
    private static final int INITIAL_BACKOFF_SECONDS = 2;

    private final OutboxStore outbox;
    private final RabbitTemplate rabbit;
    private final OutboxProperties properties;
    private final OutboxMetrics metrics;
    private final Clock clock;

    public OutboxPublisher(OutboxStore outbox, RabbitTemplate rabbit, OutboxProperties properties,
                           OutboxMetrics metrics, Clock clock) {
        this.outbox = outbox;
        this.rabbit = rabbit;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    public void publishBatch() {
        var startedAt = System.nanoTime();
        try {
            var now = clock.instant();
            var claims = outbox.claimReady(properties.batchSize(), properties.instanceId(), now,
                now.plus(properties.claimTimeout()));
            for (var claim : claims) {
                publishOne(claim);
            }
        } catch (RuntimeException exception) {
            log.error("Outbox batch failed exceptionType={}", exception.getClass().getSimpleName());
        } finally {
            metrics.batchDuration(Duration.ofNanos(System.nanoTime() - startedAt));
            try {
                metrics.refresh();
            } catch (RuntimeException exception) {
                log.error("Outbox metrics refresh failed exceptionType={}", exception.getClass().getSimpleName());
            }
        }
    }

    private void publishOne(OutboxClaim claim) {
        if (claim.recovered()) metrics.expiredClaim();
        if (claim.attempts() > properties.maxAttempts()) {
            if (outbox.markFailed(claim.eventId(), claim.claimToken(), "ATTEMPTS_EXHAUSTED")) {
                log.error("Outbox retries exhausted eventId={} jobId={} attempts={} errorCode={}",
                    claim.eventId(), claim.jobId(), claim.attempts() - 1, "ATTEMPTS_EXHAUSTED");
            }
            return;
        }
        metrics.attempt();
        try {
            publish(claim);
        } catch (PublishFailure failure) {
            if ("NEGATIVE_CONFIRM".equals(failure.code())) metrics.negativeConfirm();
            recordFailure(claim, failure.code());
            return;
        } catch (RuntimeException exception) {
            recordFailure(claim, "PUBLISH_ERROR");
            return;
        }

        try {
            if (outbox.markPublished(claim.eventId(), claim.claimToken(), clock.instant())) {
                metrics.published();
            } else {
                log.warn("Outbox claim was replaced after publish eventId={} jobId={}",
                    claim.eventId(), claim.jobId());
            }
        } catch (RuntimeException exception) {
            // Leave the claim to expire; it can be safely published again with the same eventId.
            log.error("Could not mark published eventId={} jobId={} exceptionType={}",
                claim.eventId(), claim.jobId(), exception.getClass().getSimpleName());
        }
    }

    private void publish(OutboxClaim claim) {
        var correlation = new CorrelationData(claim.eventId().toString());
        try {
            rabbit.convertAndSend(OutboxMessagingConfiguration.EXCHANGE, claim.routingKey(), claim.payload(), message -> {
                var messageProperties = message.getMessageProperties();
                messageProperties.setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
                messageProperties.setContentType("application/json");
                messageProperties.setMessageId(claim.eventId().toString());
                messageProperties.setCorrelationId(claim.correlationId().toString());
                return message;
            }, correlation);
            var confirm = correlation.getFuture().get(properties.confirmTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!confirm.isAck()) throw new PublishFailure("NEGATIVE_CONFIRM");
            if (correlation.getReturned() != null) throw new PublishFailure("UNROUTABLE");
        } catch (TimeoutException exception) {
            throw new PublishFailure("CONFIRM_TIMEOUT");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PublishFailure("PUBLISH_INTERRUPTED");
        } catch (ExecutionException | AmqpException exception) {
            throw new PublishFailure("PUBLISH_ERROR");
        }
    }

    private void recordFailure(OutboxClaim claim, String errorCode) {
        var attempt = claim.attempts();
        if (attempt >= properties.maxAttempts()) {
            if (outbox.markFailed(claim.eventId(), claim.claimToken(), errorCode)) {
                log.error("Outbox retries exhausted eventId={} jobId={} attempts={} errorCode={}",
                    claim.eventId(), claim.jobId(), attempt, errorCode);
            }
            return;
        }
        var nextAttempt = clock.instant().plus(backoff(attempt));
        outbox.scheduleRetry(claim.eventId(), claim.claimToken(), nextAttempt, errorCode);
        log.warn("Outbox publish failed eventId={} jobId={} attempt={} errorCode={} nextAttemptAt={}",
            claim.eventId(), claim.jobId(), attempt, errorCode, nextAttempt);
    }

    private static Duration backoff(int attempt) {
        var seconds = INITIAL_BACKOFF_SECONDS;
        for (var index = 1; index < attempt && seconds < MAX_BACKOFF_SECONDS; index++) {
            seconds = Math.min(MAX_BACKOFF_SECONDS, seconds * 2);
        }
        return Duration.ofSeconds(seconds);
    }

    private static final class PublishFailure extends RuntimeException {
        private final String code;

        private PublishFailure(String code) {
            this.code = code;
        }

        private String code() { return code; }
    }
}
