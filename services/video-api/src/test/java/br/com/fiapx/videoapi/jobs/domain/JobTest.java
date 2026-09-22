package br.com.fiapx.videoapi.jobs.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JobTest {

    @Test
    void progressesFromPendingToProcessingAndCompleted() {
        var job = job(JobStatus.PENDING);

        job.apply(JobStatus.PROCESSING, null);
        job.apply(JobStatus.COMPLETED, "videos/result.mp4");

        assertThat(job.status()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.resultKey()).isEqualTo("videos/result.mp4");
    }

    @Test
    void allowsFailureFromPending() {
        var job = job(JobStatus.PENDING);

        job.apply(JobStatus.FAILED, null);

        assertThat(job.status()).isEqualTo(JobStatus.FAILED);
    }

    @Test
    void allowsFailureFromProcessing() {
        var job = job(JobStatus.PROCESSING);

        job.apply(JobStatus.FAILED, null);

        assertThat(job.status()).isEqualTo(JobStatus.FAILED);
    }

    @Test
    void rejectsInvalidTransitions() {
        var completed = job(JobStatus.COMPLETED);

        assertThatThrownBy(() -> completed.apply(JobStatus.FAILED, null))
            .isInstanceOf(InvalidJobTransitionException.class)
            .hasMessage("Invalid job transition");
    }

    @Test
    void acceptsCompletionDirectlyFromPendingWhenResultArrivesOutOfOrder() {
        var pending = job(JobStatus.PENDING);

        pending.apply(JobStatus.COMPLETED, "videos/result.mp4");

        assertThat(pending.status()).isEqualTo(JobStatus.COMPLETED);
        assertThat(pending.resultKey()).isEqualTo("videos/result.mp4");
    }

    @Test
    void identifiesTerminalStatuses() {
        assertThat(JobStatus.PENDING.isTerminal()).isFalse();
        assertThat(JobStatus.PROCESSING.isTerminal()).isFalse();
        assertThat(JobStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(JobStatus.FAILED.isTerminal()).isTrue();
    }

    private Job job(JobStatus status) {
        return new Job(UUID.randomUUID(), UUID.randomUUID(), "videos/source.mp4", null, status, Instant.EPOCH);
    }
}
