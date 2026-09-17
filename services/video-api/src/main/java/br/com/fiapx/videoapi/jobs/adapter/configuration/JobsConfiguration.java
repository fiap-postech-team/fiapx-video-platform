package br.com.fiapx.videoapi.jobs.adapter.configuration;

import br.com.fiapx.videoapi.jobs.application.CreateJob;
import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.application.port.out.JobCreationIdempotencyStore;
import br.com.fiapx.videoapi.outbox.application.port.out.OutboxStore;
import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import br.com.fiapx.videoapi.inbox.application.ProcessJobResult;
import br.com.fiapx.videoapi.inbox.application.port.out.InboxStore;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JobsConfiguration {
    @Bean Clock clock() { return Clock.systemUTC(); }
    CreateJob createJob(JobStore jobs, OutboxStore outbox, VideoStore videos, Clock clock) {
        return new CreateJob(jobs, outbox, videos, clock);
    }
    @Bean CreateJob createJob(JobStore jobs, OutboxStore outbox, VideoStore videos,
                              JobCreationIdempotencyStore idempotency, Clock clock) {
        return new CreateJob(jobs, outbox, videos, idempotency, clock);
    }
    @Bean ProcessJobResult processJobResult(InboxStore inbox, JobStore jobs) { return new ProcessJobResult(inbox, jobs); }
}
