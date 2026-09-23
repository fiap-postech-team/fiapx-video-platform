package br.com.fiapx.videoprocessor.processing.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Media work requested for a single job. {@code correlationId} defaults to the job id so that
 * every event emitted for this job can be traced back to the original request.
 */
public record VideoJob(UUID jobId, UUID userId, String sourceKey, UUID correlationId) {

    public VideoJob {
        Objects.requireNonNull(jobId, "jobId is required");
        Objects.requireNonNull(userId, "userId is required");
        if (sourceKey == null || sourceKey.isBlank()) {
            throw new IllegalArgumentException("sourceKey is required");
        }
        correlationId = correlationId == null ? jobId : correlationId;
    }

    public VideoJob(UUID jobId, UUID userId, String sourceKey) {
        this(jobId, userId, sourceKey, jobId);
    }

    public ResultLocation resultLocation() {
        return ResultLocation.forJob(jobId);
    }
}
