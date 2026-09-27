package br.com.fiapx.videoapi.inbox.adapter.in.rabbit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import br.com.fiapx.videoapi.jobs.domain.JobStatus;

class JobResultMessageParserTest {
    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");

    @Test
    void parsesResultAndUsesClockWhenOccurredAtIsAbsent() {
        var eventId = UUID.randomUUID();
        var jobId = UUID.randomUUID();
        var parser = new JobResultMessageParser(new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));

        var event = parser.parse("""
            {"eventId":"%s","jobId":"%s","type":"FAILED","reason":"PROCESSING_ERROR"}
            """.formatted(eventId, jobId));

        assertThat(event.eventId()).isEqualTo(eventId);
        assertThat(event.jobId()).isEqualTo(jobId);
        assertThat(event.status()).isEqualTo(JobStatus.FAILED);
        assertThat(event.reasonCode()).isEqualTo("PROCESSING_ERROR");
        assertThat(event.occurredAt()).isEqualTo(NOW);
        assertThat(event.fingerprint()).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void rejectsMalformedResult() {
        var parser = new JobResultMessageParser(new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> parser.parse("{\"jobId\":\"bad\"}"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Invalid job result event");
    }

    @Test
    void defaultsVersionAndCorrelationAndAcceptsCompletedResult() {
        var parser = new JobResultMessageParser(new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
        var event = parser.parse("""
            {"eventId":"%s","jobId":"%s","type":"COMPLETED","resultKey":"result.zip"}
            """.formatted(UUID.randomUUID(), UUID.randomUUID()));
        assertThat(event.schemaVersion()).isOne();
        assertThat(event.correlationId()).isEqualTo(event.jobId());
        assertThat(event.resultKey()).isEqualTo("result.zip");
    }

    @Test
    void acceptsProcessorSchemaVersionOnePointZero() {
        var parser = new JobResultMessageParser(new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
        var event = parser.parse("""
            {"eventId":"%s","jobId":"%s","type":"COMPLETED","schemaVersion":"1.0","resultKey":"results/job/frames.zip"}
            """.formatted(UUID.randomUUID(), UUID.randomUUID()));
        assertThat(event.schemaVersion()).isOne();
        assertThat(event.resultKey()).isEqualTo("results/job/frames.zip");
    }

    @Test
    void rejectsUnsupportedVersionAndConditionalFields() {
        var parser = new JobResultMessageParser(new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
        var eventId = UUID.randomUUID();
        var jobId = UUID.randomUUID();
        assertThatThrownBy(() -> parser.parse("""
            {"eventId":"%s","jobId":"%s","type":"FAILED","schemaVersion":2}
            """.formatted(eventId, jobId))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> parser.parse("""
            {"eventId":"%s","jobId":"%s","type":"COMPLETED"}
            """.formatted(eventId, jobId))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validatesRoutingKeyFromAmqpProperties() {
        var parser = new JobResultMessageParser(new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
        var body = "{\"eventId\":\"" + UUID.randomUUID() + "\",\"jobId\":\"" + UUID.randomUUID() + "\",\"type\":\"FAILED\"}";
        var properties = new MessageProperties();
        properties.setReceivedRoutingKey("video.job.completed.v1");
        assertThatThrownBy(() -> parser.parse(new Message(body.getBytes(), properties)))
            .isInstanceOf(IllegalStateException.class);
    }
}
