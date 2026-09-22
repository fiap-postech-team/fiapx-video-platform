package br.com.fiapx.videoapi.jobs.domain;

public final class InvalidJobTransitionException extends IllegalStateException {
    public InvalidJobTransitionException(JobStatus current, JobStatus requested) {
        super("Invalid job transition");
    }
}
