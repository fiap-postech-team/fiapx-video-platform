package br.com.fiapx.videoapi.jobs.application.port.out;

import br.com.fiapx.videoapi.jobs.domain.Job;
import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import br.com.fiapx.videoapi.inbox.domain.JobResultEvent;
import java.util.Optional;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

public interface JobStore {
    Job save(Job job);
    Optional<Job> findOwned(UUID id, UUID userId);
    default Optional<Job> findForVideo(UUID userId, UUID videoId) {
        return Optional.empty();
    }
    default List<Job> findOwnedPage(UUID userId, Instant createdBefore, UUID idBefore, int limit) {
        throw new UnsupportedOperationException();
    }
    default List<Job> findOwnedPage(UUID userId, Instant createdBefore, UUID idBefore,
                                    JobStatus status, int limit) {
        var page = findOwnedPage(userId, createdBefore, idBefore, limit);
        return status == null ? page : page.stream().filter(job -> job.status() == status).toList();
    }
    default List<Job> findAwaitingLocalDemo(int limit) {
        return List.of();
    }
    default void applyResult(JobResultEvent event) {
        throw new UnsupportedOperationException();
    }

    default Optional<Job> findVisibleByVideoId(UUID videoId) {
        throw new UnsupportedOperationException();
    }
}
