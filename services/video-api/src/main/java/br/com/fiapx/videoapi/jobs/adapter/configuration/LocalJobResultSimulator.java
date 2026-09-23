package br.com.fiapx.videoapi.jobs.adapter.configuration;

import br.com.fiapx.videoapi.inbox.application.ProcessJobResult;
import br.com.fiapx.videoapi.inbox.domain.JobResultEvent;
import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.application.port.out.UuidGenerator;
import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Local-only demonstration of asynchronous processing. This does not invoke a media processor.
 */
@Component
@Profile("local")
@ConditionalOnProperty(prefix = "app.video", name = "local-result-simulator-enabled", havingValue = "true")
final class LocalJobResultSimulator {
    private final JobStore jobs;
    private final ProcessJobResult processJobResult;
    private final Clock clock;
    private final TransactionTemplate transactions;
    private final UuidGenerator ids;
    private final JobStatus terminalStatus;

    LocalJobResultSimulator(JobStore jobs, ProcessJobResult processJobResult, Clock clock,
                            VideoLocalDemoProperties properties, TransactionTemplate transactions,
                            UuidGenerator ids) {
        this.jobs = jobs;
        this.processJobResult = processJobResult;
        this.clock = clock;
        this.transactions = transactions;
        this.ids = ids;
        this.terminalStatus = properties.resultStatus();
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
        });
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
