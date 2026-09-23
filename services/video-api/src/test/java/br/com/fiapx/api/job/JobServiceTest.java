package br.com.fiapx.api.job;

import br.com.fiapx.api.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.*;

import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobServiceTest {
    private final JobRepository jobs = mock(JobRepository.class);
    private final JobService service = new JobService(jobs, mock(OutboxRepository.class), new ObjectMapper());

    @Test
    void getOwnedReturnsJobForOwner() {
        Job job = new Job(UUID.randomUUID(), "videos/a.mp4");
        when(jobs.findByIdAndUserId(job.getId(), job.getUserId())).thenReturn(Optional.of(job));

        assertThat(service.getOwned(job.getId(), job.getUserId())).isSameAs(job);
    }

    @Test
    void getOwnedThrowsNotFoundForAnotherOwner() {
        UUID jobId = UUID.randomUUID();
        when(jobs.findByIdAndUserId(eq(jobId), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOwned(jobId, UUID.randomUUID()))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void listOwnedDelegatesToOwnerScopedQuery() {
        UUID owner = UUID.randomUUID();
        when(jobs.findAllByUserIdOrderByCreatedAtDesc(owner)).thenReturn(List.of());

        service.listOwned(owner);

        verify(jobs).findAllByUserIdOrderByCreatedAtDesc(owner);
        verify(jobs, never()).findAll();
    }

    @Test
    void searchCombinesOwnerAndStatusFilters() {
        UUID owner = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);
        service.search(owner, Job.Status.PENDING, pageable);
        verify(jobs).findByUserIdAndStatus(owner, Job.Status.PENDING, pageable);
    }

    @Test
    void getAnyThrowsNotFoundForMissingJob() {
        UUID id = UUID.randomUUID();
        when(jobs.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAny(id)).isInstanceOf(JobNotFoundException.class);
    }
}
