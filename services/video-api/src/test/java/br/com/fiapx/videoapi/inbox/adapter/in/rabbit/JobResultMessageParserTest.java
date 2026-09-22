package br.com.fiapx.videoapi.inbox.adapter.in.rabbit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
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
}
