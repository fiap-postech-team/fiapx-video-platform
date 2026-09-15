package br.com.fiapx.videoapi.jobs.application;

import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.domain.Job;
import br.com.fiapx.videoapi.outbox.application.port.out.OutboxStore;
import java.time.Clock;
import java.util.UUID;

public final class CreateJob {
    private final JobStore jobs;
    private final OutboxStore outbox;
    private final Clock clock;

    public CreateJob(JobStore jobs, OutboxStore outbox, Clock clock) {
        this.jobs = jobs; this.outbox = outbox; this.clock = clock;
    }

    public Job execute(UUID userId, String sourceKey) {
        var job = jobs.save(new Job(UUID.randomUUID(), userId, sourceKey, clock.instant()));
        outbox.append(UUID.randomUUID(), job.id(), userId, sourceKey);
        return job;
    }
}
