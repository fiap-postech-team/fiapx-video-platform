package br.com.fiapx.videoapi.foundation.observability;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BusinessMetricsTest {
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final BusinessMetrics metrics = new BusinessMetrics(registry);

    @AfterEach
    void close() { registry.close(); }

    @Test
    void recordsDomainCountersAndTerminalDurationWithoutIdentifiersAsTags() {
        metrics.uploadCreated();
        metrics.uploadConfirmed();
        metrics.uploadExpired();
        metrics.jobCreated();
        metrics.jobFinished(JobStatus.COMPLETED, Duration.ofSeconds(12));
        metrics.jobFinished(JobStatus.FAILED, Duration.ofSeconds(5));
        metrics.publicationFailure("NEGATIVE_CONFIRM");
        metrics.consumptionFailure("RETRY");

        assertThat(registry.get("fiapx.uploads.created.events").counter().count()).isEqualTo(1);
        assertThat(registry.get("fiapx.uploads.confirmed").counter().count()).isEqualTo(1);
        assertThat(registry.get("fiapx.uploads.expired").counter().count()).isEqualTo(1);
        assertThat(registry.get("fiapx.jobs.created.events").counter().count()).isEqualTo(1);
        assertThat(registry.get("fiapx.jobs.completed").counter().count()).isEqualTo(1);
        assertThat(registry.get("fiapx.jobs.failed").counter().count()).isEqualTo(1);
        assertThat(registry.get("fiapx.jobs.duration").timer().count()).isEqualTo(2);
        assertThat(registry.get("fiapx.event.publication.failures").tag("error_code", "NEGATIVE_CONFIRM")
            .counter().count()).isEqualTo(1);
        assertThat(registry.get("fiapx.event.consumption.failures").tag("failure_stage", "RETRY")
            .counter().count()).isEqualTo(1);
        assertThat(registry.getMeters()).allSatisfy(meter ->
            assertThat(meter.getId().getTags()).noneMatch(tag ->
                tag.getKey().toLowerCase().contains("id")));
    }

    @Test
    void clampsFailureDimensionsToKnownValues() {
        metrics.publicationFailure("untrusted-input");
        metrics.consumptionFailure("untrusted-input");
        assertThat(registry.get("fiapx.event.publication.failures").tag("error_code", "PUBLISH_ERROR")
            .counter().count()).isEqualTo(1);
        assertThat(registry.get("fiapx.event.consumption.failures").tag("failure_stage", "REJECTED")
            .counter().count()).isEqualTo(1);
    }
}
