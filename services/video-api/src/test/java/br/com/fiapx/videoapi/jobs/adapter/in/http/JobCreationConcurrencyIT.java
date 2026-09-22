package br.com.fiapx.videoapi.jobs.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiapx.videoapi.jobs.application.CreateJob;
import br.com.fiapx.videoapi.jobs.application.ProcessingAlreadyExistsException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
    "app.jobs.result-listener-enabled=false",
    "spring.rabbitmq.listener.simple.auto-startup=false",
    "app.outbox.poll-interval-ms=3600000"
})
@ActiveProfiles("local")
@Testcontainers(disabledWithoutDocker = true)
class JobCreationConcurrencyIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired JdbcTemplate jdbc;
    @Autowired CreateJob createJob;
    @Autowired TransactionTemplate transactions;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void onlyOneConcurrentRequestCreatesTheSingleJobForAVideo() throws Exception {
        var userId = UUID.randomUUID();
        var videoId = UUID.randomUUID();
        var sourceKey = "users/" + userId + "/videos/" + videoId + "/source";
        seedUser(userId);
        seedUploadedVideo(userId, videoId, sourceKey);

        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> createConcurrently(start, userId, sourceKey, "request-a"));
            var second = pool.submit(() -> createConcurrently(start, userId, sourceKey, "request-b"));
            start.countDown();

            assertThat(first.get(10, TimeUnit.SECONDS) ^ second.get(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(jdbc.queryForObject("select count(*) from jobs where video_id = ?", Integer.class, videoId))
            .isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from outbox_events", Integer.class)).isEqualTo(1);
    }

    private boolean createConcurrently(CountDownLatch start, UUID userId, String sourceKey, String key)
        throws InterruptedException {
        start.await();
        try {
            transactions.execute(status -> {
                createJob.execute(userId, sourceKey, key);
                return true;
            });
            return true;
        } catch (ProcessingAlreadyExistsException exception) {
            return false;
        }
    }

    private void seedUser(UUID userId) {
        jdbc.update("insert into users (id, email, status) values (?, ?, 'ACTIVE')",
            userId, userId + "@example.test");
        jdbc.update("insert into user_credentials (user_id, password_hash, password_algorithm) values (?, ?, 'bcrypt')",
            userId, "{bcrypt}hash");
        jdbc.update("insert into user_roles (user_id, role) values (?, 'USER')", userId);
    }

    private void seedUploadedVideo(UUID userId, UUID videoId, String sourceKey) {
        var now = Instant.now();
        jdbc.update("""
            insert into videos (id, user_id, object_key, original_filename, declared_content_type, size_bytes,
                checksum_sha256, upload_status, created_at, updated_at, expires_at, uploaded_at)
            values (?, ?, ?, 'source.mp4', 'video/mp4', 1, ?, 'UPLOADED', ?, ?, ?, ?)
            """, videoId, userId, sourceKey, "a".repeat(64), Timestamp.from(now), Timestamp.from(now),
            Timestamp.from(now.plusSeconds(86_400)), Timestamp.from(now));
    }
}
