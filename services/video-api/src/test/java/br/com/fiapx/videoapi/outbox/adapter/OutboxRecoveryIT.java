package br.com.fiapx.videoapi.outbox.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import br.com.fiapx.videoapi.outbox.adapter.configuration.OutboxMessagingConfiguration;
import br.com.fiapx.videoapi.outbox.application.port.out.OutboxStore;
import br.com.fiapx.videoapi.outbox.adapter.configuration.OutboxProperties;
import br.com.fiapx.videoapi.outbox.adapter.observability.OutboxMetrics;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=1",
    "spring.rabbitmq.username=guest",
    "spring.rabbitmq.password=guest",
    "app.outbox.poll-interval-ms=3600000"
})
@ActiveProfiles("local")
@Testcontainers(disabledWithoutDocker = true)
class OutboxRecoveryIT {
    private static final String USERNAME = "outboxtest";
    private static final String PASSWORD = "outboxtest-secret";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired JdbcTemplate jdbc;
    @Autowired OutboxStore outbox;
    @Autowired OutboxPublisher publisher;
    @Autowired RabbitTemplate rabbit;
    @Autowired RabbitAdmin rabbitAdmin;
    @Autowired OutboxMetrics metrics;
    @Autowired ObjectMapper json;
    private final TestRestTemplate client = new TestRestTemplate();
    @LocalServerPort int port;
    private RabbitMQContainer broker;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @AfterEach
    void stopBroker() {
        if (broker != null) broker.stop();
    }

    @Test
    void acceptsJobWithoutBrokerAndPublishesTheSameOutboxEventAfterRecovery() throws Exception {
        var token = createUser();
        var ownerId = currentUserId(token);
        var videoId = UUID.randomUUID();
        var sourceKey = "users/" + ownerId + "/" + videoId + "/source";
        var now = Timestamp.from(Instant.now());
        jdbc.update("""
            insert into videos (id, user_id, object_key, original_filename, declared_content_type, size_bytes,
                checksum_sha256, upload_status, created_at, updated_at, expires_at, uploaded_at)
            values (?, ?, ?, 'source.mp4', 'video/mp4', 1, ?, 'UPLOADED', ?, ?, ?, ?)
            """, videoId, ownerId, sourceKey, "a".repeat(64), now, now,
            Timestamp.from(Instant.now().plusSeconds(86_400)), now);

        var response = postJob(token, sourceKey);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var jobId = UUID.fromString(response.getBody().get("id").toString());
        var eventId = jdbc.queryForObject("select id from outbox_events where aggregate_id = ?", UUID.class, jobId);
        assertThat(jdbc.queryForObject("select status from outbox_events where id = ?", String.class, eventId))
            .isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("select jsonb_typeof(payload_json) from outbox_events where id = ?",
            String.class, eventId)).isEqualTo("object");

        startBrokerAndReconnect();
        awaitPublished(eventId);

        var published = rabbit.receive(OutboxMessagingConfiguration.QUEUE, 5000);
        assertThat(published).isNotNull();
        assertThat(published.getMessageProperties().getReceivedDeliveryMode())
            .isEqualTo(MessageDeliveryMode.PERSISTENT);
        var body = new String(published.getBody());
        assertThat(body).contains(eventId.toString(), jobId.toString(), sourceKey,
            "video.job.requested.v1", "schemaVersion", "correlationId", "occurredAt");
        var envelope = json.readTree(body);
        assertThat(envelope.path("eventId").asText()).isEqualTo(eventId.toString());
        assertThat(envelope.path("jobId").asText()).isEqualTo(jobId.toString());
        assertThat(envelope.path("userId").asText()).isEqualTo(ownerId.toString());
        assertThat(envelope.path("sourceKey").asText()).isEqualTo(sourceKey);
        assertThat(envelope.path("type").asText()).isEqualTo("video.job.requested.v1");
        assertThat(envelope.path("schemaVersion").asInt()).isEqualTo(1);
        assertThat(envelope.path("occurredAt").isTextual()).isTrue();
        assertThat(envelope.path("correlationId").asText()).isEqualTo(jobId.toString());
        assertThat(jdbc.queryForObject("select status from outbox_events where id = ?", String.class, eventId))
            .isEqualTo("PUBLISHED");

        publishesAgainAfterConfirmBeforeOutboxMarking();
        timesOutWaitingForBrokerConfirm();
        forceNackOnFullQueue();
        var rejectedEventId = UUID.randomUUID();
        var rejectedJobId = UUID.randomUUID();
        outbox.append(rejectedEventId, rejectedJobId, ownerId, null, sourceKey, Instant.now());
        publisher.publishBatch();
        assertThat(jdbc.queryForObject("select status from outbox_events where id = ?", String.class,
            rejectedEventId)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("select last_error_code from outbox_events where id = ?", String.class,
            rejectedEventId)).isEqualTo("NEGATIVE_CONFIRM");
        assertThat(jdbc.queryForObject("select next_attempt_at > now() + interval '1 second' "
            + "from outbox_events where id = ?", Boolean.class, rejectedEventId)).isTrue();
    }

    private void publishesAgainAfterConfirmBeforeOutboxMarking() throws Exception {
        var eventId = UUID.randomUUID();
        var jobId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var occurredAt = Instant.now();
        var sourceKey = "users/duplicate/source.mp4";
        outbox.append(eventId, jobId, userId, null, sourceKey, occurredAt);
        var claim = outbox.claimReady(1, "crashed-api", occurredAt, occurredAt.plusSeconds(30)).getFirst();
        var sent = new CorrelationData(eventId.toString());
        rabbit.convertAndSend(OutboxMessagingConfiguration.EXCHANGE, claim.routingKey(), claim.payload(), message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setMessageId(eventId.toString());
            message.getMessageProperties().setCorrelationId(jobId.toString());
            message.getMessageProperties().setContentType("application/json");
            return message;
        }, sent);
        assertThat(sent.getFuture().get(5, TimeUnit.SECONDS).isAck()).isTrue();
        jdbc.update("update outbox_events set claim_expires_at = now() - interval '1 second' where id = ?", eventId);

        publisher.publishBatch();

        assertThat(jdbc.queryForObject("select status from outbox_events where id = ?", String.class, eventId))
            .isEqualTo("PUBLISHED");
        var firstDelivery = rabbit.receive(OutboxMessagingConfiguration.QUEUE, 5000);
        var duplicateDelivery = rabbit.receive(OutboxMessagingConfiguration.QUEUE, 5000);
        assertThat(firstDelivery).isNotNull();
        assertThat(duplicateDelivery).isNotNull();
        assertThat(firstDelivery.getMessageProperties().getMessageId()).isEqualTo(eventId.toString());
        assertThat(duplicateDelivery.getMessageProperties().getMessageId()).isEqualTo(eventId.toString());
    }

    private void timesOutWaitingForBrokerConfirm() throws Exception {
        var eventId = UUID.randomUUID();
        var jobId = UUID.randomUUID();
        outbox.append(eventId, jobId, UUID.randomUUID(), null, "users/timeout/source.mp4", Instant.now());
        var neverConfirmingRabbit = mock(RabbitTemplate.class);
        var shortWaitPublisher = new OutboxPublisher(outbox, neverConfirmingRabbit,
            new OutboxProperties(1, 1000, Duration.ofSeconds(30), Duration.ofMillis(50), 10, "timeout-test"),
            metrics, Clock.systemUTC());

        shortWaitPublisher.publishBatch();

        assertThat(jdbc.queryForObject("select status from outbox_events where id = ?", String.class, eventId))
            .isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("select last_error_code from outbox_events where id = ?", String.class,
            eventId)).isEqualTo("CONFIRM_TIMEOUT");
        Thread.sleep(2100);
        publisher.publishBatch();
        assertThat(jdbc.queryForObject("select status from outbox_events where id = ?", String.class, eventId))
            .isEqualTo("PUBLISHED");
        assertThat(jdbc.queryForObject("select last_error_code from outbox_events where id = ?", String.class,
            eventId)).isNull();

        var delivery = rabbit.receive(OutboxMessagingConfiguration.QUEUE, 5000);
        assertThat(delivery).isNotNull();
        assertThat(delivery.getMessageProperties().getMessageId()).isEqualTo(eventId.toString());
        Message extra;
        while ((extra = rabbit.receive(OutboxMessagingConfiguration.QUEUE, 200)) != null) {
            assertThat(extra.getMessageProperties().getMessageId()).isEqualTo(eventId.toString());
        }
    }

    private void startBrokerAndReconnect() {
        broker = new RabbitMQContainer("rabbitmq:4-management-alpine");
        broker.start();
        execRabbit("add_user", USERNAME, PASSWORD);
        execRabbit("set_permissions", "-p", "/", USERNAME, ".*", ".*", ".*");
        var connectionFactory = (CachingConnectionFactory) rabbit.getConnectionFactory();
        connectionFactory.setAddresses(broker.getHost() + ":" + broker.getAmqpPort());
        connectionFactory.setUsername(USERNAME);
        connectionFactory.setPassword(PASSWORD);
        connectionFactory.resetConnection();
        rabbitAdmin.initialize();
    }

    private void awaitPublished(UUID eventId) throws InterruptedException {
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            publisher.publishBatch();
            if ("PUBLISHED".equals(jdbc.queryForObject("select status from outbox_events where id = ?",
                    String.class, eventId))) {
                return;
            }
            Thread.sleep(250);
        }
        assertThat(jdbc.queryForObject("select status from outbox_events where id = ?", String.class, eventId))
            .isEqualTo("PUBLISHED");
    }

    private void execRabbit(String... arguments) {
        try {
            var command = new String[arguments.length + 1];
            command[0] = "rabbitmqctl";
            System.arraycopy(arguments, 0, command, 1, arguments.length);
            var result = broker.execInContainer(command);
            assertThat(result.getExitCode()).isZero();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not configure RabbitMQ test user", exception);
        }
    }

    private void forceNackOnFullQueue() throws Exception {
        execRabbit("set_policy", "--apply-to", "queues", "reject-publish",
            "^video\\.processing\\.v1$", "{\"max-length\":1,\"overflow\":\"reject-publish\"}");
        var correlation = new CorrelationData(UUID.randomUUID().toString());
        rabbit.convertAndSend(OutboxMessagingConfiguration.EXCHANGE, "video.job.requested.v1", "filler",
            message -> persistent(message), correlation);
        assertThat(correlation.getFuture().get(5, TimeUnit.SECONDS).isAck()).isTrue();
    }

    private static Message persistent(Message message) {
        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        return message;
    }

    private String createUser() {
        var credentials = Map.of("email", "outbox-" + UUID.randomUUID() + "@example.test", "password", PASSWORD);
        client.postForEntity(url("/v1/auth/register"), credentials, Map.class);
        return (String) client.postForEntity(url("/v1/auth/login"), credentials, Map.class)
            .getBody().get("accessToken");
    }

    private UUID currentUserId(String token) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var response = client.exchange(url("/v1/me"), HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        return UUID.fromString(response.getBody().get("id").toString());
    }

    private org.springframework.http.ResponseEntity<Map> postJob(String token, String sourceKey) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return client.exchange(url("/v1/jobs"), HttpMethod.POST,
            new HttpEntity<>(Map.of("sourceKey", sourceKey), headers), Map.class);
    }

    private String url(String path) { return "http://localhost:" + port + path; }
}
