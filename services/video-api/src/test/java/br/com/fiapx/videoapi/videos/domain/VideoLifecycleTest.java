package br.com.fiapx.videoapi.videos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VideoLifecycleTest {
    private static final Instant CREATED = Instant.parse("2026-09-19T00:00:00Z");

    @Test
    void confirmsOnceAndKeepsTheFirstConfirmationTime() {
        var pending = video(CREATED.plusSeconds(86400));

        var uploaded = pending.confirm(CREATED.plusSeconds(60));

        assertThat(uploaded.confirm(CREATED.plusSeconds(90))).isEqualTo(uploaded);
        assertThat(uploaded.uploadedAt()).isEqualTo(CREATED.plusSeconds(60));
    }

    @Test
    void normalizesConfirmationTimestampToPostgresPrecision() {
        var instant = CREATED.plusSeconds(60).plusNanos(123456789);

        var uploaded = video(CREATED.plusSeconds(86400)).confirm(instant);

        assertThat(uploaded.uploadedAt()).isEqualTo(CREATED.plusSeconds(60).plusNanos(123456000));
    }

    @Test
    void expiresPendingVideoAndRejectsLateConfirmation() {
        var pending = video(CREATED.plusSeconds(86400));

        var expired = pending.expire(CREATED.plusSeconds(86400));

        assertThat(expired.status()).isEqualTo(VideoStatus.EXPIRED);
        assertThatThrownBy(() -> pending.confirm(CREATED.plusSeconds(86400)))
            .isInstanceOf(IllegalStateException.class);
    }

    private Video video(Instant expiresAt) {
        return new Video(UUID.randomUUID(), UUID.randomUUID(), "users/u/videos/v/source", "clip.mp4",
            "video/mp4", 10, null, VideoStatus.PENDING, CREATED, CREATED, expiresAt, null, null);
    }
}
