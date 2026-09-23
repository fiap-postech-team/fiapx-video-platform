package br.com.fiapx.videoapi.jobs.domain;

public enum JobStatus {
    PENDING, PROCESSING, COMPLETED, FAILED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
