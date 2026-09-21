package br.com.fiapx.videoapi.jobs.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiapx.videoapi.jobs.application.CreateJob;
import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.domain.Job;
import br.com.fiapx.videoapi.identity.domain.AuthenticatedIdentity;
import br.com.fiapx.videoapi.identity.domain.UserRole;
import br.com.fiapx.videoapi.outbox.application.port.out.OutboxStore;
import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import br.com.fiapx.videoapi.videos.domain.Video;
import br.com.fiapx.videoapi.videos.domain.VideoStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JobControllerTest {

    @Test
    void createsAJobForTheAuthenticatedSubject() {
        var jobs = new InMemoryJobStore();
        var userId = UUID.randomUUID();
        var controller = controller(jobs);

        var response = controller.create(new JobController.CreateJobRequest("uploads/source.mp4"), null, jwt(userId));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().userId()).isEqualTo(userId);
        assertThat(response.getBody().sourceKey()).isEqualTo("uploads/source.mp4");
    }

    @Test
    void returnsOnlyAJobOwnedByTheAuthenticatedSubject() {
        var jobs = new InMemoryJobStore();
        var ownerId = UUID.randomUUID();
        var job = new Job(UUID.randomUUID(), ownerId, "uploads/source.mp4", Instant.EPOCH);
        jobs.save(job);

        var response = controller(jobs).get(job.id(), jwt(ownerId));

        assertThat(response.id()).isEqualTo(job.id());
    }

    @Test
    void hidesJobsOwnedByOtherUsers() {
        var jobs = new InMemoryJobStore();
        var job = new Job(UUID.randomUUID(), UUID.randomUUID(), "uploads/source.mp4", Instant.EPOCH);
        jobs.save(job);

        assertThatThrownBy(() -> controller(jobs).get(job.id(), jwt(UUID.randomUUID())))
            .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void listsJobsWithoutCursorWhenPageIsIncomplete() {
        var jobs = new InMemoryJobStore();
        var userId = UUID.randomUUID();
        jobs.save(new Job(UUID.randomUUID(), userId, "uploads/one.mp4", Instant.EPOCH));

        var page = controller(jobs).list(null, 20, jwt(userId));

        assertThat(page.items()).singleElement().extracting(JobController.JobResponse::sourceKey).isEqualTo("uploads/one.mp4");
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void listsJobsWithNextCursorWhenPageLimitIsReached() {
        var jobs = new InMemoryJobStore();
        var userId = UUID.randomUUID();
        var older = new Job(UUID.randomUUID(), userId, "uploads/older.mp4", Instant.EPOCH);
        var newer = new Job(UUID.randomUUID(), userId, "uploads/newer.mp4", Instant.EPOCH.plusSeconds(60));
        jobs.save(older);
        jobs.save(newer);

        var page = controller(jobs).list(null, 2, jwt(userId));

        assertThat(page.items()).hasSize(2);
        assertThat(JobCursor.decode(page.nextCursor())).isEqualTo(new JobCursor(older.createdAt(), older.id()));
    }

    @Test
    void rejectsInvalidPageSize() {
        assertThatThrownBy(() -> controller(new InMemoryJobStore()).list(null, 0, jwt(UUID.randomUUID())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid page size");
        assertThatThrownBy(() -> controller(new InMemoryJobStore()).list(null, 101, jwt(UUID.randomUUID())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid page size");
    }

    @Test
    void rejectsOverlongIdempotencyKey() {
        assertThatThrownBy(() -> controller(new InMemoryJobStore())
            .create(new JobController.CreateJobRequest("uploads/source.mp4"), "x".repeat(129), jwt(UUID.randomUUID())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid idempotency key");
    }

    @Test
    void treatsBlankIdempotencyKeyAsMissing() {
        var jobs = new InMemoryJobStore();
        var userId = UUID.randomUUID();

        var response = controller(jobs).create(new JobController.CreateJobRequest("uploads/source.mp4"), "   ", jwt(userId));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().userId()).isEqualTo(userId);
    }

    private JobController controller(InMemoryJobStore jobs) {
        return new JobController(
            new CreateJob(jobs, (eventId, jobId, userId, sourceKey) -> { }, videos(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)),
            jobs
        );
    }

    private VideoStore videos() {
        return new VideoStore() {
            public Optional<Video> findConfirmed(UUID user, String key) {
                return Optional.of(new Video(UUID.randomUUID(), user, key, "source.mp4", "video/mp4", 1,
                    "a".repeat(64), VideoStatus.UPLOADED, Instant.EPOCH, Instant.EPOCH));
            }
            public Optional<Video> lock(UUID videoId) {
                return findConfirmed(UUID.randomUUID(), "uploads/source.mp4");
            }
            public Video save(Video video) { return video; }
        };
    }

    private AuthenticatedIdentity jwt(UUID subject) {
        return new AuthenticatedIdentity(subject, UUID.randomUUID(), java.util.Set.of(UserRole.USER));
    }

    private static final class InMemoryJobStore implements JobStore {
        private final List<Job> jobs = new ArrayList<>();
        public Job save(Job candidate) {
            jobs.removeIf(job -> job.id().equals(candidate.id()));
            jobs.add(candidate);
            jobs.sort(java.util.Comparator.comparing(Job::createdAt).reversed().thenComparing(Job::id));
            return candidate;
        }
        public Optional<Job> findOwned(UUID id, UUID userId) {
            return jobs.stream()
                .filter(job -> job.id().equals(id) && job.userId().equals(userId))
                .findFirst();
        }
        public List<Job> findOwnedPage(UUID userId, Instant createdBefore, UUID idBefore, int limit) {
            return jobs.stream()
                .filter(job -> job.userId().equals(userId))
                .filter(job -> createdBefore == null
                    || job.createdAt().isBefore(createdBefore)
                    || (job.createdAt().equals(createdBefore) && job.id().compareTo(idBefore) < 0))
                .limit(limit)
                .toList();
        }
        public Optional<Job> findVisibleByVideoId(UUID videoId) {
            return jobs.stream()
                .filter(job -> videoId.equals(job.videoId()) && job.libraryVisible())
                .findFirst();
        }
    }
}
