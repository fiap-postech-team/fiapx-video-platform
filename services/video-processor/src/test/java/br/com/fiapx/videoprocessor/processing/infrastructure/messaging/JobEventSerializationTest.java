package br.com.fiapx.videoprocessor.processing.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiapx.videoprocessor.processing.domain.JobEvent;
import br.com.fiapx.videoprocessor.processing.domain.VideoJob;
import br.com.fiapx.videoprocessor.processing.infrastructure.messaging.in.JobRequestedMessage;
import br.com.fiapx.videoprocessor.processing.infrastructure.messaging.out.JobResultMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Guards the JSON that actually travels between services against the shapes in
 * {@code contracts/asyncapi.yaml}.
 */
class JobEventSerializationTest {

    private final ObjectMapper objectMapper = new RabbitMessagingConfig().objectMapper();

    @Test
    void writesTimestampsAsIsoInstantsAndOmitsAbsentOptionalFields() throws Exception {
        VideoJob job = new VideoJob(UUID.randomUUID(), UUID.randomUUID(), "uploads/video.mp4");
        JobEvent event = JobEvent.processing(job, Instant.parse("2026-08-30T20:00:00Z"));

        String json = objectMapper.writeValueAsString(JobResultMessage.from(event));

        assertThat(json)
                .contains("\"type\":\"PROCESSING\"")
                .contains("\"schemaVersion\":\"1.0\"")
                .contains("\"occurredAt\":\"2026-08-30T20:00:00Z\"")
                .contains("\"correlationId\":\"" + job.correlationId() + "\"")
                .doesNotContain("resultKey")
                .doesNotContain("terminal")
                .doesNotContain("reason");
    }

    @Test
    void ignoresOptionalEnvelopeFieldsTheProducerMayAddToAV1Request() throws Exception {
        UUID jobId = UUID.randomUUID();
        String json =
                """
                {
                  "eventId": "bbf73e16-ab32-441b-9020-adf2fc6b4425",
                  "jobId": "%s",
                  "userId": "5f2f2f96-2f4f-4f8f-8f6f-2f4f6f8f0f2f",
                  "sourceKey": "uploads/video.mp4",
                  "occurredAt": "2026-08-30T19:59:00Z",
                  "schemaVersion": "1.0",
                  "somethingAddedLater": true
                }
                """
                        .formatted(jobId);

        JobRequestedMessage message = objectMapper.readValue(json, JobRequestedMessage.class);

        assertThat(message.jobId()).isEqualTo(jobId);
        assertThat(message.sourceKey()).isEqualTo("uploads/video.mp4");
        assertThat(message.correlationId()).isNull();
    }
}
