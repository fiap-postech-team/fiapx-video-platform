package br.com.fiapx.videoapi.jobs.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiapx.videoapi.jobs.application.port.out.JobCreationIdempotencyStore;
import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.domain.Job;
import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import br.com.fiapx.videoapi.outbox.application.port.out.OutboxStore;
import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import br.com.fiapx.videoapi.videos.domain.Video;
import br.com.fiapx.videoapi.videos.domain.VideoStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateJobTest {

    @Test
    void persistsPendingJobAndRecordsRequestedEvent() {
        var jobs = new RecordingJobStore();
        var outbox = new RecordingOutboxStore();
        var clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        var userId = UUID.randomUUID();

        var job = new CreateJob(jobs, outbox, videos(userId), clock).execute(userId, "uploads/source.mp4");

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

    @Test
    void returnsTheSameJobForTheSameIdempotencyKey() {
        var jobs = new RecordingJobStore();
        var outbox = new RecordingOutboxStore();
        var userId = UUID.randomUUID();
        var useCase = new CreateJob(jobs, outbox, videos(userId), new IdempotencyStore(), Clock.systemUTC());

        var first = useCase.execute(userId, "uploads/source.mp4", "request-1");
        var repeated = useCase.execute(userId, "uploads/source.mp4", "request-1");

        assertThat(repeated.id()).isEqualTo(first.id());
        assertThat(outbox.appended).isEqualTo(1);
    }

    @Test
    void rejectsAChangedRequestForAnExistingIdempotencyKey() {
        var jobs = new RecordingJobStore();
        var userId = UUID.randomUUID();
        var useCase = new CreateJob(jobs, new RecordingOutboxStore(), videos(userId), new IdempotencyStore(), Clock.systemUTC());
        useCase.execute(userId, "uploads/source.mp4", "request-1");

        assertThatThrownBy(() -> useCase.execute(userId, "uploads/other.mp4", "request-1"))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    private VideoStore videos(UUID userId) {
        var video = new Video(UUID.randomUUID(), userId, "uploads/source.mp4", "source.mp4", "video/mp4", 1,
            "a".repeat(64), VideoStatus.UPLOADED, Instant.EPOCH, Instant.EPOCH);
        return new VideoStore() {
            public Optional<Video> findConfirmed(UUID owner, String key) { return Optional.of(video); }
            public Video save(Video candidate) { return candidate; }
        };
    }

    private static final class RecordingJobStore implements JobStore {
        private Job saved;
        public Job save(Job job) { saved = job; return job; }
        public Optional<Job> findOwned(UUID id, UUID userId) {
            return saved != null && saved.id().equals(id) && saved.userId().equals(userId)
                    ? Optional.of(saved) : Optional.empty();
        }
    }

    private static final class RecordingOutboxStore implements OutboxStore {
        private UUID eventId;
        private UUID jobId;
        private UUID userId;
        private String sourceKey;
        private int appended;
        public void append(UUID eventId, UUID jobId, UUID userId, String sourceKey) {
            this.eventId = eventId; this.jobId = jobId; this.userId = userId; this.sourceKey = sourceKey;
            appended++;
        }
    }

    private static final class IdempotencyStore implements JobCreationIdempotencyStore {
        private final Map<String, RecordedJobCreation> entries = new HashMap<>();

        public void lock(UUID userId, String key) {
        }

        public Optional<RecordedJobCreation> find(UUID userId, String key) {
            return Optional.ofNullable(entries.get(userId + key));
        }

        public void record(UUID userId, String key, String fingerprint, UUID jobId) {
            entries.put(userId + key, new RecordedJobCreation(fingerprint, jobId));
        }
    }
}
