package br.com.fiapx.videoapi.jobs.adapter.configuration;

import br.com.fiapx.videoapi.identity.application.port.out.IdentityStore;
import br.com.fiapx.videoapi.inbox.application.ProcessJobResult;
import br.com.fiapx.videoapi.inbox.domain.JobResultEvent;
import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.application.port.out.UuidGenerator;
import br.com.fiapx.videoapi.jobs.domain.Job;
import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import br.com.fiapx.videoapi.outbox.adapter.configuration.OutboxMessagingConfiguration;
import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Local-only demonstration of asynchronous processing. This does not invoke a media processor.
 * A terminal transition is also published so the notification worker can send the same email
 * the processor would send. The API inbox already recorded the event, so the result consumer
 * ignores the copy.
 */
@Component
@Profile("local")
@ConditionalOnProperty(prefix = "app.video", name = "local-result-simulator-enabled", havingValue = "true")
final class LocalJobResultSimulator {
    private static final Logger log = LoggerFactory.getLogger(LocalJobResultSimulator.class);

    private final JobStore jobs;
    private final ProcessJobResult processJobResult;
    private final Clock clock;
    private final TransactionTemplate transactions;
    private final UuidGenerator ids;
    private final JobStatus terminalStatus;
    private final IdentityStore identities;
    private final VideoStore videos;
    private final RabbitTemplate rabbit;
    private final ObjectMapper json;

    LocalJobResultSimulator(JobStore jobs, ProcessJobResult processJobResult, Clock clock,
                            VideoLocalDemoProperties properties, TransactionTemplate transactions,
                            UuidGenerator ids, IdentityStore identities, VideoStore videos,
                            RabbitTemplate rabbit, ObjectMapper json) {
        this.jobs = jobs;
        this.processJobResult = processJobResult;
        this.clock = clock;
        this.transactions = transactions;
        this.ids = ids;
        this.terminalStatus = properties.resultStatus();
        this.identities = identities;
        this.videos = videos;
        this.rabbit = rabbit;
        this.json = json;
    }

    @Scheduled(fixedDelayString = "${app.video.local-result-simulator-delay:PT2S}")
    void simulateResult() {
        jobs.findAwaitingLocalDemo(25).forEach(job -> {
            var next = job.status() == JobStatus.PENDING ? JobStatus.PROCESSING : terminalStatus;
            var event = new JobResultEvent(
                ids.next(), job.id(), next, null,
                next == JobStatus.FAILED ? "LOCAL_DEMO_FAILURE" : null,
                clock.instant(), fingerprint(job.id() + ":" + next)
            );
            transactions.executeWithoutResult(status -> processJobResult.execute(event));
            if (next.isTerminal()) {
                publishNotification(job, event);
            }
        });
    }

    private void publishNotification(Job job, JobResultEvent event) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("eventId", event.eventId());
        payload.put("jobId", event.jobId());
        payload.put("type", event.status().name());
        payload.put("schemaVersion", "1.0");
        payload.put("occurredAt", event.occurredAt().toString());
        payload.put("correlationId", event.correlationId());
        payload.put("terminal", true);
        if (event.status() == JobStatus.COMPLETED) {
            payload.put("resultKey", "local-demo/" + job.id() + "/frames.zip");
        }
        identities.findUser(job.userId()).map(user -> user.email())
            .filter(email -> email != null && !email.isBlank())
            .ifPresent(email -> payload.put("recipient", email.trim()));
        if (job.videoId() != null) {
            videos.findOwnedById(job.userId(), job.videoId())
                .map(video -> video.originalFilename())
                .filter(name -> name != null && !name.isBlank())
                .ifPresent(name -> payload.put("videoName", name.trim()));
        }
        try {
            var body = json.writeValueAsString(payload);
            rabbit.convertAndSend(OutboxMessagingConfiguration.EXCHANGE, event.routingKey(), body, message -> {
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                message.getMessageProperties().setMessageId(event.eventId().toString());
                return message;
            });
            log.info("published local {} notification for job {}", event.status(), job.id());
        } catch (RuntimeException exception) {
            log.warn("could not publish local {} notification for job {}", event.status(), job.id(), exception);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            log.warn("could not serialize local {} notification for job {}", event.status(), job.id(), exception);
        }
    }

    private static String fingerprint(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }
}
