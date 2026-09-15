package br.com.fiapx.videoapi.jobs.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.fiapx.videoapi.jobs.domain.Job;
import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JpaJobStoreTest {

    @Test
    void mapsSavedDomainJobToJpaAndBack() {
        var repository = mock(SpringDataJobRepository.class);
        when(repository.save(any(JobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var job = job();

        var saved = new JpaJobStore(repository).save(job);

        assertThat(saved.id()).isEqualTo(job.id());
        assertThat(saved.userId()).isEqualTo(job.userId());
        assertThat(saved.status()).isEqualTo(JobStatus.PENDING);
    }

    @Test
    void mapsOwnedJobWhenRepositoryFindsIt() {
        var repository = mock(SpringDataJobRepository.class);
        var job = job();
        when(repository.findByIdAndUserId(job.id(), job.userId())).thenReturn(Optional.of(new JobEntity(job)));

        var found = new JpaJobStore(repository).findOwned(job.id(), job.userId());

        assertThat(found).isPresent().get().extracting(Job::sourceKey).isEqualTo("uploads/source.mp4");
    }

    @Test
    void returnsEmptyWhenNoOwnedJobExists() {
        var repository = mock(SpringDataJobRepository.class);
        when(repository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        assertThat(new JpaJobStore(repository).findOwned(UUID.randomUUID(), UUID.randomUUID())).isEmpty();
    }

    private Job job() {
        return new Job(UUID.randomUUID(), UUID.randomUUID(), "uploads/source.mp4", null, JobStatus.PENDING, Instant.EPOCH);
    }
}
