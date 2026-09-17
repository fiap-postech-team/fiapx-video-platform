package br.com.fiapx.videoapi.jobs.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiapx.videoapi.jobs.application.CreateJob;
import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.domain.Job;
import br.com.fiapx.videoapi.identity.domain.AuthenticatedIdentity;
import br.com.fiapx.videoapi.identity.domain.UserRole;
import br.com.fiapx.videoapi.outbox.application.port.out.OutboxStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JobControllerTest {

    @Test
    void createsAJobForTheAuthenticatedSubject() {
        var jobs = new InMemoryJobStore();
        var userId = UUID.randomUUID();
        var controller = controller(jobs);

        var response = controller.create(new JobController.CreateJobRequest("uploads/source.mp4"), jwt(userId));

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

    private JobController controller(InMemoryJobStore jobs) {
        return new JobController(
            new CreateJob(jobs, (eventId, jobId, userId, sourceKey) -> { }, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)),
            jobs
        );
    }

    private AuthenticatedIdentity jwt(UUID subject) {
        return new AuthenticatedIdentity(subject, UUID.randomUUID(), java.util.Set.of(UserRole.USER));
    }

    private static final class InMemoryJobStore implements JobStore {
        private Job job;
        public Job save(Job candidate) { job = candidate; return candidate; }
        public Optional<Job> findOwned(UUID id, UUID userId) {
            return job != null && job.id().equals(id) && job.userId().equals(userId) ? Optional.of(job) : Optional.empty();
        }
    }
}
