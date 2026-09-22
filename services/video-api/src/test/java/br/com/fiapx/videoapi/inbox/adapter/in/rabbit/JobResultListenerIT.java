package br.com.fiapx.videoapi.inbox.adapter.in.rabbit;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiapx.videoapi.outbox.adapter.configuration.OutboxMessagingConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
    "app.video.local-result-simulator-enabled=false",
    "app.jobs.result-listener-enabled=true",
    "app.outbox.poll-interval-ms=3600000"
})
@ActiveProfiles("local")
@Testcontainers(disabledWithoutDocker = true)
class JobResultListenerIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4-management-alpine");

    @Autowired JdbcTemplate jdbc;
    @Autowired RabbitTemplate rabbit;
    @Autowired ObjectMapper json;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    @Test
    void consumesResultsTransactionallyAndDeduplicatesTheSameEvent() throws Exception {
        var userId = UUID.randomUUID();
        var jobId = UUID.randomUUID();
        seedJob(userId, jobId);
        var eventId = UUID.randomUUID();
        var body = json.writeValueAsString(java.util.Map.of(
            "eventId", eventId,
            "jobId", jobId,
            "type", "COMPLETED",
            "resultKey", "results/" + jobId + "/frames.zip"));

        rabbit.convertAndSend(OutboxMessagingConfiguration.EXCHANGE, "video.job.completed.v1", body);
        awaitStatus(jobId, "COMPLETED");
        rabbit.convertAndSend(OutboxMessagingConfiguration.EXCHANGE, "video.job.completed.v1", body);
        awaitInbox(eventId);

        assertThat(jdbc.queryForObject("select count(*) from inbox_events where event_id = ?", Integer.class, eventId))
            .isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from job_status_history where job_id = ?", Integer.class, jobId))
            .isEqualTo(2);
    }

    private void awaitStatus(UUID jobId, String status) throws InterruptedException {
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            var current = jdbc.queryForObject("select status from jobs where id = ?", String.class, jobId);
            if (status.equals(current)) return;
            Thread.sleep(100);
        }
        assertThat(jdbc.queryForObject("select status from jobs where id = ?", String.class, jobId)).isEqualTo(status);
    }

    private void awaitInbox(UUID eventId) throws InterruptedException {
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (jdbc.queryForObject("select count(*) from inbox_events where event_id = ?", Integer.class, eventId) == 1) return;
            Thread.sleep(100);
        }
        assertThat(jdbc.queryForObject("select count(*) from inbox_events where event_id = ?", Integer.class, eventId))
            .isEqualTo(1);
    }

    private void seedJob(UUID userId, UUID jobId) {
        var now = Instant.now();
        jdbc.update("insert into users (id, email, status) values (?, ?, 'ACTIVE')", userId, userId + "@example.test");
        jdbc.update("insert into jobs (id, user_id, source_key, status, created_at, updated_at) values (?, ?, ?, 'PENDING', ?, ?)",
            jobId, userId, "users/" + userId + "/source", Timestamp.from(now), Timestamp.from(now));
        jdbc.update("insert into job_status_history (job_id, status, occurred_at, recorded_at) values (?, 'PENDING', ?, ?)",
            jobId, Timestamp.from(now), Timestamp.from(now));
    }
}
