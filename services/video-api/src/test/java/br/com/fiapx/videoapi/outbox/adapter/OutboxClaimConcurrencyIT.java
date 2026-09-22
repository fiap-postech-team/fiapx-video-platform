package br.com.fiapx.videoapi.outbox.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiapx.videoapi.outbox.application.port.out.OutboxStore;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=1",
    "spring.rabbitmq.username=guest",
    "spring.rabbitmq.password=guest",
    "app.outbox.poll-interval-ms=3600000"
})
@ActiveProfiles("local")
@Testcontainers(disabledWithoutDocker = true)
class OutboxClaimConcurrencyIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired OutboxStore outbox;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void clearEvents() {
        jdbc.update("delete from outbox_events");
    }

    @Test
    void twoInstancesCannotClaimTheSameEventAtOnce() throws Exception {
        var eventId = append(Instant.now());
        var start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> claimAfter(start, "api-a"));
            var second = workers.submit(() -> claimAfter(start, "api-b"));
            start.countDown();

            var claims = List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
            assertThat(claims.stream().mapToInt(List::size).sum()).isEqualTo(1);
            assertThat(claims.stream().flatMap(List::stream).map(claim -> claim.eventId()))
                .containsExactly(eventId);
        }
    }

    @Test
    void reclaimsExpiredClaimsAndReportsTheirRecovery() {
        var eventId = append(Instant.now());
        var token = UUID.randomUUID();
        jdbc.update("update outbox_events set status = 'PROCESSING', attempts = 1, claim_token = ?, "
            + "claimed_by = 'crashed-api', claimed_at = now() - interval '1 minute', "
            + "claim_expires_at = now() - interval '1 second' where id = ?", token, eventId);

        var recovered = outbox.claimReady(1, "recovery-api", Instant.now(), Instant.now().plusSeconds(30));

        assertThat(recovered).hasSize(1);
        assertThat(recovered.getFirst().eventId()).isEqualTo(eventId);
        assertThat(recovered.getFirst().attempts()).isEqualTo(2);
        assertThat(recovered.getFirst().recovered()).isTrue();
        assertThat(jdbc.queryForObject("select claimed_by from outbox_events where id = ?", String.class, eventId))
            .isEqualTo("recovery-api");
    }

    @Test
    void skipsAClaimLockedByAnotherTransaction() throws Exception {
        var oldestEventId = append(Instant.parse("2026-09-22T12:00:00Z"));
        var nextEventId = append(Instant.parse("2026-09-22T12:00:01Z"));

        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            try (ResultSet ignored = statement.executeQuery(
                    "select id from outbox_events where id = '" + oldestEventId + "' for update")) {
                assertThat(ignored.next()).isTrue();
            }

            var claim = outbox.claimReady(1, "api-other", Instant.now(), Instant.now().plusSeconds(30));
            assertThat(claim).hasSize(1);
            assertThat(claim.getFirst().eventId()).isEqualTo(nextEventId);
            connection.rollback();
        }
    }

    private List<br.com.fiapx.videoapi.outbox.domain.OutboxClaim> claimAfter(CountDownLatch start, String instance)
            throws InterruptedException {
        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
        var now = Instant.now();
        return outbox.claimReady(1, instance, now, now.plusSeconds(30));
    }

    private UUID append(Instant occurredAt) {
        var eventId = UUID.randomUUID();
        outbox.append(eventId, UUID.randomUUID(), UUID.randomUUID(), null,
            "users/test/source.mp4", occurredAt);
        return eventId;
    }
}
