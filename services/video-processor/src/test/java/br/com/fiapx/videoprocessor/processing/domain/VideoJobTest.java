package br.com.fiapx.videoprocessor.processing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class VideoJobTest {

    @Test
    void fallsBackToTheJobIdWhenTheProducerSendsNoCorrelationId() {
        UUID jobId = UUID.randomUUID();

        VideoJob job = new VideoJob(jobId, UUID.randomUUID(), "uploads/video.mp4", null);

        assertThat(job.correlationId()).isEqualTo(jobId);
    }

    @Test
    void keepsTheCorrelationIdSentByTheProducer() {
        UUID correlationId = UUID.randomUUID();

        VideoJob job = new VideoJob(UUID.randomUUID(), UUID.randomUUID(), "uploads/video.mp4", correlationId);

        assertThat(job.correlationId()).isEqualTo(correlationId);
    }

    @Test
    void rejectsAPayloadMissingContractRequiredFields() {
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> new VideoJob(null, userId, "uploads/video.mp4"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new VideoJob(jobId, null, "uploads/video.mp4"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new VideoJob(jobId, userId, " ")).isInstanceOf(IllegalArgumentException.class);
    }
}
