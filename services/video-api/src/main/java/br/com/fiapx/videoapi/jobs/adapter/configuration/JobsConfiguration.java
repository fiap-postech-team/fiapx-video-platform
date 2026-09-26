package br.com.fiapx.videoapi.jobs.adapter.configuration;

import br.com.fiapx.videoapi.jobs.application.CreateJob;
import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.application.port.out.JobCreationIdempotencyStore;
import br.com.fiapx.videoapi.outbox.application.port.out.OutboxStore;
import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import br.com.fiapx.videoapi.inbox.application.ProcessJobResult;
import br.com.fiapx.videoapi.inbox.application.port.out.InboxStore;
import br.com.fiapx.videoapi.identity.application.port.out.IdentityStore;
import br.com.fiapx.videoapi.identity.domain.UserStatus;
import br.com.fiapx.videoapi.jobs.application.port.out.UuidGenerator;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import br.com.fiapx.videoapi.jobs.application.DownloadResult;
import br.com.fiapx.videoapi.videos.application.port.out.VideoObjectStorage;
import br.com.fiapx.videoapi.videos.adapter.configuration.VideoUploadProperties;
import br.com.fiapx.videoapi.foundation.observability.BusinessMetrics;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({VideoLocalDemoProperties.class, JobProperties.class})
public class JobsConfiguration {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean UuidGenerator uuidGenerator() { return UUID::randomUUID; }
    CreateJob createJob(JobStore jobs, OutboxStore outbox, VideoStore videos, Clock clock) {
        return new CreateJob(jobs, outbox, videos, clock);
    }
    @Bean CreateJob createJob(JobStore jobs, OutboxStore outbox, VideoStore videos,
                              JobCreationIdempotencyStore idempotency, IdentityStore identities,
                              Clock clock, UuidGenerator ids) {
        return new CreateJob(jobs, outbox, videos, idempotency,
            userId -> identities.findUser(userId).map(user -> user.status() == UserStatus.ACTIVE).orElse(false),
            clock, ids);
    }
    @Bean ProcessJobResult processJobResult(InboxStore inbox, JobStore jobs, BusinessMetrics metrics) {
        return new ProcessJobResult(inbox, jobs, metrics);
    }
    @Bean DownloadResult downloadResult(JobStore jobs, VideoStore videos, VideoObjectStorage storage,
                                        VideoUploadProperties properties) {
        return new DownloadResult(jobs, videos, storage, properties.resultDownloadUrlTtl(), properties.resultDownloadMockUrl());
    }
}
