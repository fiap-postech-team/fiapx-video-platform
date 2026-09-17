package br.com.fiapx.videoapi.jobs.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** Persists and serializes job creation requests identified by a client key. */
public interface JobCreationIdempotencyStore {
    void lock(UUID userId, String key);
    Optional<RecordedJobCreation> find(UUID userId, String key);
    void record(UUID userId, String key, String requestFingerprint, UUID jobId);

    record RecordedJobCreation(String requestFingerprint, UUID jobId) {
    }
}
