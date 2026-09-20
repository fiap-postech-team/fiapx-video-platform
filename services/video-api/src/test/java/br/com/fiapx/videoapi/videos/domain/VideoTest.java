package br.com.fiapx.videoapi.videos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VideoTest {

    @Test
    void confirmsPendingVideos() {
        var createdAt = Instant.EPOCH;
        var updatedAt = createdAt.plusSeconds(1);
        var now = createdAt.plusSeconds(5);
        var video = new Video(UUID.randomUUID(), UUID.randomUUID(), "uploads/source.mp4", "source.mp4",
            "video/mp4", 1, "a".repeat(64), VideoStatus.PENDING, createdAt, updatedAt);

        var confirmed = video.confirm(now);

        assertThat(video.isConfirmed()).isFalse();
        assertThat(confirmed.isConfirmed()).isTrue();
        assertThat(confirmed.status()).isEqualTo(VideoStatus.UPLOADED);
        assertThat(confirmed.updatedAt()).isEqualTo(now);
    }

    @Test
    void repeatedConfirmationKeepsTheOriginalResult() {
        var video = new Video(UUID.randomUUID(), UUID.randomUUID(), "uploads/source.mp4", "source.mp4",
            "video/mp4", 1, "a".repeat(64), VideoStatus.UPLOADED, Instant.EPOCH, Instant.EPOCH);

        assertThat(video.confirm(Instant.EPOCH.plusSeconds(5))).isSameAs(video);
    }
}
