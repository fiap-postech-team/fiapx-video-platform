package br.com.fiapx.api.job;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import static org.assertj.core.api.Assertions.assertThat;

/** Proves ownership filtering happens in the database, not in memory. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class JobRepositoryOwnershipTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JobRepository jobs;

    @Test
    void findByIdAndUserIdMatchesOnlyTheOwner() {
        Job job = jobs.save(new Job(UUID.randomUUID(), "videos/owned.mp4"));

        assertThat(jobs.findByIdAndUserId(job.getId(), job.getUserId())).isPresent();
        assertThat(jobs.findByIdAndUserId(job.getId(), UUID.randomUUID())).isEmpty();
    }

    @Test
    void findAllByUserIdReturnsOnlyOwnedJobs() {
        UUID owner = UUID.randomUUID();
        jobs.save(new Job(owner, "videos/one.mp4"));
        jobs.save(new Job(owner, "videos/two.mp4"));
        jobs.save(new Job(UUID.randomUUID(), "videos/other.mp4"));

        assertThat(jobs.findAllByUserIdOrderByCreatedAtDesc(owner))
                .hasSize(2)
                .allMatch(job -> job.getUserId().equals(owner));
    }
}
