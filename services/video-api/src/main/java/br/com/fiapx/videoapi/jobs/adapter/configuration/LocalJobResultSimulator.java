package br.com.fiapx.videoapi.jobs.adapter.configuration;

import br.com.fiapx.videoapi.inbox.application.ProcessJobResult;
import br.com.fiapx.videoapi.inbox.domain.JobResultEvent;
import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import java.time.Clock;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
    private final JobStatus terminalStatus;

    LocalJobResultSimulator(JobStore jobs, ProcessJobResult processJobResult, Clock clock,
                            VideoLocalDemoProperties properties) {
        this.jobs = jobs;
        this.processJobResult = processJobResult;
        this.clock = clock;
        this.terminalStatus = properties.resultStatus();
    }

    @Scheduled(fixedDelayString = "${app.video.local-result-simulator-delay:PT2S}")
    void simulateResult() {
        jobs.findAwaitingLocalDemo(25).forEach(job -> {
            var next = job.status() == JobStatus.PENDING ? JobStatus.PROCESSING : terminalStatus;
            processJobResult.execute(new JobResultEvent(
                UUID.randomUUID(), job.id(), next, null,
                next == JobStatus.FAILED ? "LOCAL_DEMO_FAILURE" : null,
                clock.instant(), "local-demo:" + job.id() + ":" + next
            ));
        });
    }
}
