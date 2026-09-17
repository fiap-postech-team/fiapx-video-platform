package br.com.fiapx.videoapi.jobs.adapter.out.persistence;

import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.domain.Job;
import br.com.fiapx.videoapi.inbox.domain.JobResultEvent;
import java.util.Optional;
import java.util.List;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class JpaJobStore implements JobStore {
    private final SpringDataJobRepository repository;
    private final JobStatusHistoryRepository history;
    public JpaJobStore(SpringDataJobRepository repository, JobStatusHistoryRepository history) { this.repository = repository; this.history = history; }
    public Job save(Job job) {
        var saved = repository.save(new JobEntity(job)).toDomain();
        history.save(new JobStatusHistoryEntity(saved.id(), saved.status().name(), null, null, saved.createdAt()));
        return saved;
    }
    public Optional<Job> findOwned(UUID id, UUID userId) { return repository.findByIdAndUserId(id, userId).map(JobEntity::toDomain); }
    public List<Job> findOwnedPage(UUID userId, Instant createdBefore, UUID idBefore, int limit) {
        return repository.findOwnedPage(userId, createdBefore, idBefore, limit).stream().map(JobEntity::toDomain).toList();
    }
    public void applyResult(JobResultEvent event) {
        var entity = repository.findById(event.jobId()).orElseThrow();
        var job = entity.toDomain();
        if (job.status().isTerminal() || job.status() == event.status()) return;
        job.apply(event.status(), event.resultKey()); entity.apply(job);
        history.save(new JobStatusHistoryEntity(job.id(), job.status().name(), event.eventId(), event.reasonCode(), event.occurredAt()));
    }
}
