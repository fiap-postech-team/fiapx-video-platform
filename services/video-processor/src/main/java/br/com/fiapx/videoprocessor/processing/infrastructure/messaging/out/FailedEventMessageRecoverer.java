package br.com.fiapx.videoprocessor.processing.infrastructure.messaging.out;

import br.com.fiapx.videoprocessor.processing.application.port.out.JobEventPublisher;
import br.com.fiapx.videoprocessor.processing.domain.JobEvent;
import br.com.fiapx.videoprocessor.processing.infrastructure.messaging.in.JobRequestedMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.stereotype.Component;

/**
 * Last step of the bounded retry chain. Transient failures are retried by the container; once the
 * attempts are exhausted the failure is terminal for this job, so the event catalog's terminal
 * failure is published before the message is dead-lettered for operator inspection.
 *
 * <p>The published reason is deliberately generic: {@code cause} may hold a stack trace, a command
 * line or storage details that must not reach users.
 */
@Component
public class FailedEventMessageRecoverer implements MessageRecoverer {

    private static final Logger log = LoggerFactory.getLogger(FailedEventMessageRecoverer.class);

    static final String REASON = "Video processing failed after repeated attempts";

    private final JobEventPublisher publisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public FailedEventMessageRecoverer(JobEventPublisher publisher, ObjectMapper objectMapper, Clock clock) {
        this.publisher = publisher;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void recover(Message message, Throwable cause) {
        JobRequestedMessage request = readRequest(message);
        if (request != null && request.jobId() != null) {
            log.error("job {} exhausted processing retries, dead-lettering", request.jobId(), cause);
            publisher.publish(JobEvent.failed(
                    request.jobId(), request.correlationId(), REASON, true, clock.instant(),
                    request.recipient(), request.videoName()));
        } else {
            log.error("dead-lettering a message without a readable job id", cause);
        }
        throw new AmqpRejectAndDontRequeueException("processing retries exhausted", cause);
    }

    private JobRequestedMessage readRequest(Message message) {
        try {
            return objectMapper.readValue(message.getBody(), JobRequestedMessage.class);
        } catch (Exception e) {
            log.warn("could not read the dead-lettered payload as video.job.requested.v1", e);
            return null;
        }
    }
}
