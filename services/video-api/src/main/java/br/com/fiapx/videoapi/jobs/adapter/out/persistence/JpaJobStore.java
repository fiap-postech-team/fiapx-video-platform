package br.com.fiapx.videoapi.jobs.adapter.out.persistence;

import br.com.fiapx.videoapi.jobs.application.ProcessingAlreadyExistsException;
import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.domain.Job;
import br.com.fiapx.videoapi.inbox.domain.JobResultEvent;
import java.util.Optional;
import java.util.List;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaJobStore implements JobStore {
    private final SpringDataJobRepository repository;
    private final JobStatusHistoryRepository history;
    private final java.time.Clock clock;
    public JpaJobStore(SpringDataJobRepository repository, JobStatusHistoryRepository history) {
        this(repository, history, java.time.Clock.systemUTC());
    }
    @Autowired
    public JpaJobStore(SpringDataJobRepository repository, JobStatusHistoryRepository history, java.time.Clock clock) {
        this.repository = repository; this.history = history; this.clock = clock;
    }
    public Job save(Job job) {
        try {
            var savedEntity = repository.save(new JobEntity(job));
            repository.flush();
            var saved = savedEntity.toDomain();
            history.save(new JobStatusHistoryEntity(saved.id(), saved.status().name(), null, null,
                saved.createdAt(), clock.instant()));
            return saved;
        } catch (DataIntegrityViolationException exception) {
            if (videoJobConflict(exception)) {
                throw new ProcessingAlreadyExistsException();
            }
            throw exception;
        }
    }
    public Optional<Job> findOwned(UUID id, UUID userId) { return repository.findByIdAndUserId(id, userId).map(JobEntity::toDomain); }
    public Optional<Job> findForResult(UUID id) { return repository.findById(id).map(JobEntity::toDomain); }
    public Optional<Job> findVisibleByVideoId(UUID videoId) {
        return repository.findByVideoIdAndVideoLibraryVisibleIsTrue(videoId).map(JobEntity::toDomain);
    }
    public List<Job> findOwnedPage(UUID userId, Instant createdBefore, UUID idBefore, int limit) {
        return findOwnedPage(userId, createdBefore, idBefore, null, limit);
    }
    public List<Job> findOwnedPage(UUID userId, Instant createdBefore, UUID idBefore,
                                   br.com.fiapx.videoapi.jobs.domain.JobStatus status, int limit) {
        var statusName = status == null ? null : status.name();
        var page = statusName == null
            ? repository.findOwnedPage(userId, createdBefore, idBefore, limit)
            : repository.findOwnedPageByStatus(userId, createdBefore, idBefore, statusName, limit);
        return page
            .stream().map(JobEntity::toDomain).toList();
    }
    @Transactional
    public void applyResult(JobResultEvent event) {
        var entity = repository.findById(event.jobId()).orElseThrow();
        var job = entity.toDomain();
        if (job.status().isTerminal() || job.status() == event.status()) return;
        job.applyResult(event.status(), event.resultKey(), event.reasonCode(), event.occurredAt());
        var recordedAt = clock.instant();
        entity.apply(job, recordedAt);
        history.save(new JobStatusHistoryEntity(job.id(), job.status().name(), event.eventId(),
            event.reasonCode(), event.occurredAt(), recordedAt));
        repository.flush();
    }

    private static boolean videoJobConflict(DataIntegrityViolationException exception) {
        var specificCause = exception.getMostSpecificCause();
        var cause = specificCause == null ? null : specificCause.getMessage();
        return cause != null && (cause.contains("jobs_one_visible_per_video")
            || cause.contains("jobs_video_once_unique_idx"));
    }
}
