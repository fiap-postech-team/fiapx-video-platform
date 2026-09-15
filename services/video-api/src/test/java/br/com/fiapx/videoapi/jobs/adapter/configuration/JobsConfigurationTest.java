package br.com.fiapx.videoapi.jobs.adapter.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.domain.Job;
import br.com.fiapx.videoapi.outbox.application.port.out.OutboxStore;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JobsConfigurationTest {

    @Test
    void providesTheClockAndCreateJobUseCase() {
        var configuration = new JobsConfiguration();
        JobStore jobs = new JobStore() {
            public Job save(Job job) { return job; }
            public Optional<Job> findOwned(UUID id, UUID userId) { return Optional.empty(); }
        };
        OutboxStore outbox = (eventId, jobId, userId, sourceKey) -> { };

        var useCase = configuration.createJob(jobs, outbox, configuration.clock());

        assertThat(configuration.clock().getZone().getId()).isEqualTo("Z");
        assertThat(useCase).isNotNull();
    }
}
