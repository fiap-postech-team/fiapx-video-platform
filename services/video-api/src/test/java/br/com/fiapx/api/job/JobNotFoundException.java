package br.com.fiapx.api.job;

import java.util.UUID;

/**
 * Raised when a job does not exist or does not belong to the acting owner.
 * Both cases are indistinguishable to callers (HTTP 404).
 */
public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException(UUID id) {
        super("Job not found: " + id);
    }
}
