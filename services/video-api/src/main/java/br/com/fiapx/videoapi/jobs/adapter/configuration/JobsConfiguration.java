package br.com.fiapx.videoapi.jobs.adapter.configuration;

import br.com.fiapx.videoapi.jobs.application.CreateJob;
import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.outbox.application.port.out.OutboxStore;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JobsConfiguration {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean CreateJob createJob(JobStore jobs, OutboxStore outbox, Clock clock) { return new CreateJob(jobs, outbox, clock); }
}
