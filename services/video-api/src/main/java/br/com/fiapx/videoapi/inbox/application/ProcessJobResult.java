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
        var found = jobs.findForResult(event.jobId());
        if (found.isEmpty()) {
            // The concrete store raises a not-found exception here; retaining this
            // fallback keeps the port compatible with lightweight adapters/tests.
            jobs.applyResult(event);
            inbox.complete(event, null);
            return;
        }
        var job = found.get();
        String ignored = null;
        if (job.status().isTerminal()) {
            ignored = job.status() == event.status() ? "ALREADY_APPLIED" : "TERMINAL_CONFLICT";
        } else if (job.status() == event.status()) {
            ignored = "ALREADY_APPLIED";
        } else {
            jobs.applyResult(event);
        }
        inbox.complete(event, ignored);
    }
}
