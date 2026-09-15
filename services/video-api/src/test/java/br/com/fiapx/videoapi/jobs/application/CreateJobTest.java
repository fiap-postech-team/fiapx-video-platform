package br.com.fiapx.videoapi.jobs.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.domain.Job;
import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import br.com.fiapx.videoapi.outbox.application.port.out.OutboxStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateJobTest {

    @Test
    void persistsPendingJobAndRecordsRequestedEvent() {
        var jobs = new RecordingJobStore();
        var outbox = new RecordingOutboxStore();
        var clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        var userId = UUID.randomUUID();

        var job = new CreateJob(jobs, outbox, clock).execute(userId, "uploads/source.mp4");

        assertThat(jobs.saved).isSameAs(job);
        assertThat(job.userId()).isEqualTo(userId);
        assertThat(job.sourceKey()).isEqualTo("uploads/source.mp4");
        assertThat(job.status()).isEqualTo(JobStatus.PENDING);
        assertThat(job.createdAt()).isEqualTo(clock.instant());
        assertThat(outbox.jobId).isEqualTo(job.id());
        assertThat(outbox.userId).isEqualTo(userId);
        assertThat(outbox.sourceKey).isEqualTo("uploads/source.mp4");
        assertThat(outbox.eventId).isNotNull();
    }

    private static final class RecordingJobStore implements JobStore {
        private Job saved;
        public Job save(Job job) { saved = job; return job; }
        public Optional<Job> findOwned(UUID id, UUID userId) { return Optional.empty(); }
    }

    private static final class RecordingOutboxStore implements OutboxStore {
        private UUID eventId;
        private UUID jobId;
        private UUID userId;
        private String sourceKey;
        public void append(UUID eventId, UUID jobId, UUID userId, String sourceKey) {
            this.eventId = eventId; this.jobId = jobId; this.userId = userId; this.sourceKey = sourceKey;
        }
    }
}
