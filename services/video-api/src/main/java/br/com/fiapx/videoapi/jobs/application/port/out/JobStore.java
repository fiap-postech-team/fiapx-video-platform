package br.com.fiapx.videoapi.jobs.application.port.out;

import br.com.fiapx.videoapi.jobs.domain.Job;
import java.util.Optional;
import java.util.UUID;

public interface JobStore {
    Job save(Job job);
    Optional<Job> findOwned(UUID id, UUID userId);
}
