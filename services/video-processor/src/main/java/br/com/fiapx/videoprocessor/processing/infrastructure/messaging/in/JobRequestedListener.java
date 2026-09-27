package br.com.fiapx.videoprocessor.processing.infrastructure.messaging.in;

import br.com.fiapx.videoprocessor.processing.application.port.in.ProcessVideoJob;
import br.com.fiapx.videoprocessor.processing.domain.VideoJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Entry point for {@code video.job.requested.v1}.
 *
 * <p>A message that does not carry the fields the contract requires can never succeed, so it is
 * rejected without retry and dead-lettered. Every other exception is left to the container, whose
 * bounded retry ends in {@code FailedEventMessageRecoverer}.
 */
@Component
public class JobRequestedListener {

    private static final Logger log = LoggerFactory.getLogger(JobRequestedListener.class);

    private final ProcessVideoJob processVideoJob;

    public JobRequestedListener(ProcessVideoJob processVideoJob) {
        this.processVideoJob = processVideoJob;
    }

    @RabbitListener(queues = "${app.messaging.queue}")
    public void onJobRequested(JobRequestedMessage message) {
        VideoJob job = toJob(message);
        log.info("received job {} for source key {}", job.jobId(), job.sourceKey());
        processVideoJob.handle(job);
    }

    private VideoJob toJob(JobRequestedMessage message) {
        if (message == null) {
            throw new AmqpRejectAndDontRequeueException("empty video.job.requested.v1 payload");
        }
        try {
            return new VideoJob(
                    message.jobId(),
                    message.userId(),
                    message.sourceKey(),
                    message.correlationId(),
                    message.recipient(),
                    message.videoName());
        } catch (RuntimeException e) {
            throw new AmqpRejectAndDontRequeueException(
                    "invalid video.job.requested.v1 payload: " + e.getMessage(), e);
        }
    }
}
