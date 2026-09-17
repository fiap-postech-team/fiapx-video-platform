package br.com.fiapx.videoapi.jobs.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiapx.videoapi.inbox.domain.JobResultEvent;
import br.com.fiapx.videoapi.jobs.domain.Job;
import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JpaJobStoreTest {

    @Test
    void mapsSavedDomainJobToJpaAndBack() {
        var repository = mock(SpringDataJobRepository.class);
        var history = mock(JobStatusHistoryRepository.class);
        when(repository.save(any(JobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var job = job();

        var saved = new JpaJobStore(repository, history).save(job);

        assertThat(saved.id()).isEqualTo(job.id());
        assertThat(saved.userId()).isEqualTo(job.userId());
        assertThat(saved.status()).isEqualTo(JobStatus.PENDING);
    }

    @Test
    void mapsOwnedJobWhenRepositoryFindsIt() {
        var repository = mock(SpringDataJobRepository.class);
        var history = mock(JobStatusHistoryRepository.class);
        var job = job();
        when(repository.findByIdAndUserId(job.id(), job.userId())).thenReturn(Optional.of(new JobEntity(job)));

        var found = new JpaJobStore(repository, history).findOwned(job.id(), job.userId());

        assertThat(found).isPresent().get().extracting(Job::sourceKey).isEqualTo("uploads/source.mp4");
    }

    @Test
    void returnsEmptyWhenNoOwnedJobExists() {
        var repository = mock(SpringDataJobRepository.class);
        var history = mock(JobStatusHistoryRepository.class);
        when(repository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        assertThat(new JpaJobStore(repository, history).findOwned(UUID.randomUUID(), UUID.randomUUID())).isEmpty();
    }

    @Test
    void mapsOwnedPageResults() {
        var repository = mock(SpringDataJobRepository.class);
        var history = mock(JobStatusHistoryRepository.class);
        var first = job();
        var second = new Job(UUID.randomUUID(), first.userId(), "uploads/second.mp4", null, JobStatus.PROCESSING, Instant.EPOCH.plusSeconds(1));
        when(repository.findOwnedPage(first.userId(), Instant.EPOCH.plusSeconds(2), first.id(), 2))
            .thenReturn(List.of(new JobEntity(first), new JobEntity(second)));

        var page = new JpaJobStore(repository, history).findOwnedPage(first.userId(), Instant.EPOCH.plusSeconds(2), first.id(), 2);

        assertThat(page).extracting(Job::sourceKey).containsExactly("uploads/source.mp4", "uploads/second.mp4");
    }

    @Test
    void appliesResultAndRecordsHistoryForNonTerminalStatusChange() {
        var repository = mock(SpringDataJobRepository.class);
        var history = mock(JobStatusHistoryRepository.class);
        var existing = new JobEntity(job());
        var event = new JobResultEvent(UUID.randomUUID(), existing.toDomain().id(), JobStatus.PROCESSING,
            null, null, Instant.EPOCH.plusSeconds(5), "fingerprint");
        when(repository.findById(event.jobId())).thenReturn(Optional.of(existing));

        new JpaJobStore(repository, history).applyResult(event);

        assertThat(existing.toDomain().status()).isEqualTo(JobStatus.PROCESSING);
        var saved = ArgumentCaptor.forClass(JobStatusHistoryEntity.class);
        verify(history).save(saved.capture());
        assertThat(ReflectionTestUtils.getField(saved.getValue(), "eventId")).isEqualTo(event.eventId());
    }

    @Test
    void ignoresDuplicateOrTerminalResults() {
        var repository = mock(SpringDataJobRepository.class);
        var history = mock(JobStatusHistoryRepository.class);
        var completed = new JobEntity(new Job(UUID.randomUUID(), UUID.randomUUID(), "uploads/source.mp4", "videos/result.mp4",
            JobStatus.COMPLETED, Instant.EPOCH));
        var duplicate = new JobEntity(new Job(UUID.randomUUID(), UUID.randomUUID(), "uploads/source.mp4", null,
            JobStatus.PROCESSING, Instant.EPOCH));
        when(repository.findById(completed.toDomain().id())).thenReturn(Optional.of(completed));
        when(repository.findById(duplicate.toDomain().id())).thenReturn(Optional.of(duplicate));

        new JpaJobStore(repository, history).applyResult(new JobResultEvent(UUID.randomUUID(), completed.toDomain().id(),
            JobStatus.FAILED, null, "ERROR", Instant.EPOCH.plusSeconds(1), "terminal"));
        new JpaJobStore(repository, history).applyResult(new JobResultEvent(UUID.randomUUID(), duplicate.toDomain().id(),
            JobStatus.PROCESSING, null, null, Instant.EPOCH.plusSeconds(1), "duplicate"));

        verify(history, never()).save(any(JobStatusHistoryEntity.class));
        assertThat(completed.toDomain().status()).isEqualTo(JobStatus.COMPLETED);
        assertThat(duplicate.toDomain().status()).isEqualTo(JobStatus.PROCESSING);
    }

    private Job job() {
        return new Job(UUID.randomUUID(), UUID.randomUUID(), "uploads/source.mp4", null, JobStatus.PENDING, Instant.EPOCH);
    }
}
