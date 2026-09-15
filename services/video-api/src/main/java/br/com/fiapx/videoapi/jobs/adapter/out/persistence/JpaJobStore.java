package br.com.fiapx.videoapi.jobs.adapter.out.persistence;

import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.domain.Job;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class JpaJobStore implements JobStore {
    private final SpringDataJobRepository repository;
    public JpaJobStore(SpringDataJobRepository repository) { this.repository = repository; }
    public Job save(Job job) { return repository.save(new JobEntity(job)).toDomain(); }
    public Optional<Job> findOwned(UUID id, UUID userId) { return repository.findByIdAndUserId(id, userId).map(JobEntity::toDomain); }
}
