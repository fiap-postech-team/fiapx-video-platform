package br.com.fiapx.videoprocessor.processing.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Object key of the ZIP produced for a job. The key is derived only from the job id, which makes a
 * repeated upload replace the previous one and lets the worker detect already finished work.
 */
public record ResultLocation(String key) {

    private static final String PREFIX = "results/";
    private static final String FILE_NAME = "/frames.zip";

    public ResultLocation {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key is required");
        }
    }

    public static ResultLocation forJob(UUID jobId) {
        Objects.requireNonNull(jobId, "jobId is required");
        return new ResultLocation(PREFIX + jobId + FILE_NAME);
    }

    @Override
    public String toString() {
        return key;
    }
}
