package br.com.fiapx.videoapi.foundation.observability;

import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** Low-cardinality counters and timers for FIAP X business operations. */
@Component
public final class BusinessMetrics {
    private final Counter uploadsCreated;
    private final Counter uploadsConfirmed;
    private final Counter uploadsExpired;
    private final Counter jobsCreated;
    private final Counter jobsCompleted;
    private final Counter jobsFailed;
    private final Timer jobDuration;
    private final MeterRegistry registry;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
        // Keep "created" away from the metric-name suffix: Prometheus reserves _created
        // for sample timestamps and otherwise strips it from counter names.
        uploadsCreated = registry.counter("fiapx.uploads.created.events");
        uploadsConfirmed = registry.counter("fiapx.uploads.confirmed");
        uploadsExpired = registry.counter("fiapx.uploads.expired");
        jobsCreated = registry.counter("fiapx.jobs.created.events");
        jobsCompleted = registry.counter("fiapx.jobs.completed");
        jobsFailed = registry.counter("fiapx.jobs.failed");
        jobDuration = registry.timer("fiapx.jobs.duration");
    }

    public void uploadCreated() { uploadsCreated.increment(); }
    public void uploadConfirmed() { uploadsConfirmed.increment(); }
    public void uploadExpired() { uploadsExpired.increment(); }
    public void jobCreated() { jobsCreated.increment(); }

    public void jobFinished(JobStatus status, Duration duration) {
        if (status == JobStatus.COMPLETED) jobsCompleted.increment();
        if (status == JobStatus.FAILED) jobsFailed.increment();
        if (status.isTerminal() && duration != null && !duration.isNegative()) jobDuration.record(duration);
    }

    public void publicationFailure(String errorCode) {
        Counter.builder("fiapx.event.publication.failures")
            .tag("error_code", safeCode(errorCode))
            .register(registry)
            .increment();
    }

    public void consumptionFailure(String failureStage) {
        Counter.builder("fiapx.event.consumption.failures")
            .tag("failure_stage", safeStage(failureStage))
            .register(registry)
            .increment();
    }

    private static String safeCode(String code) {
        return switch (code == null ? "" : code) {
            case "NEGATIVE_CONFIRM", "UNROUTABLE", "CONFIRM_TIMEOUT", "PUBLISH_INTERRUPTED", "PUBLISH_ERROR", "BATCH_ERROR" -> code;
            default -> "PUBLISH_ERROR";
        };
    }

    private static String safeStage(String stage) {
        return switch (stage == null ? "" : stage) {
            case "INVALID_MESSAGE", "RETRY", "REJECTED" -> stage;
            default -> "REJECTED";
        };
    }
}
