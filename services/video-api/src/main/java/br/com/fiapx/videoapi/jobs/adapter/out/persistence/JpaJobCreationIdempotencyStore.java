package br.com.fiapx.videoapi.jobs.adapter.out.persistence;

import br.com.fiapx.videoapi.jobs.application.port.out.JobCreationIdempotencyStore;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
final class JpaJobCreationIdempotencyStore implements JobCreationIdempotencyStore {
    private final JobCreationIdempotencyRepository repository;
    private final Clock clock;

    JpaJobCreationIdempotencyStore(JobCreationIdempotencyRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void lock(UUID userId, String key) {
        repository.lock(userId + ":" + key);
    }

    public Optional<RecordedJobCreation> find(UUID userId, String key) {
        return repository.findByIdUserIdAndIdIdempotencyKey(userId, key)
                .map(entry -> new RecordedJobCreation(entry.requestFingerprint(), entry.jobId()));
    }

    public void record(UUID userId, String key, String fingerprint, UUID jobId) {
        repository.save(new JobCreationIdempotencyEntity(userId, key, fingerprint, jobId, clock.instant()));
    }
}
