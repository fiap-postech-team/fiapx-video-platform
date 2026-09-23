package br.com.fiapx.videoapi.inbox.application;

import br.com.fiapx.videoapi.inbox.application.port.out.InboxStore;
import br.com.fiapx.videoapi.inbox.domain.JobResultEvent;
import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;

public final class ProcessJobResult {
    private final InboxStore inbox;
    private final JobStore jobs;

    public ProcessJobResult(InboxStore inbox, JobStore jobs) {
        this.inbox = inbox;
        this.jobs = jobs;
    }

    public void execute(JobResultEvent event) {
        if (!inbox.register(event)) {
            return;
        }
        jobs.applyResult(event);
    }
}
