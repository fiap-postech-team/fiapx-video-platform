package br.com.fiapx.videoapi.videos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiapx.videoapi.videos.application.port.out.VideoObjectStorage;
import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import br.com.fiapx.videoapi.videos.application.port.out.VideoTransactions;
import br.com.fiapx.videoapi.videos.domain.Video;
import br.com.fiapx.videoapi.videos.domain.VideoStatus;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class VideoUploadFailuresTest {
    private static final Instant NOW = Instant.parse("2026-09-19T12:00:00Z");
    private final VideoStore videos = mock(VideoStore.class);
    private final VideoObjectStorage storage = mock(VideoObjectStorage.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final VideoTransactions transactions = new VideoTransactions() {
        public <T> T execute(Supplier<T> operation) { return operation.get(); }
    };

    @Test
    void storageFailureDoesNotConfirmPendingVideo() {
        var video = pending();
        when(videos.lockOwned(video.userId(), video.id())).thenReturn(Optional.of(video));
        when(storage.stat(video.objectKey())).thenThrow(new StorageUnavailableException(new IOException()));

        assertThatThrownBy(() -> new ConfirmVideo(videos, storage, transactions, clock)
            .execute(video.userId(), video.id())).isInstanceOf(StorageUnavailableException.class);
        verify(videos, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void repeatedConfirmationDoesNotConsultStorageAgain() {
        var uploaded = pending().confirm(NOW);
        when(videos.lockOwned(uploaded.userId(), uploaded.id())).thenReturn(Optional.of(uploaded));

        assertThat(new ConfirmVideo(videos, storage, transactions, clock)
            .execute(uploaded.userId(), uploaded.id())).isEqualTo(uploaded);
        verify(storage, never()).stat(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void invalidDeclarationDoesNotGenerateCredential() {
        var policy = new VideoUploadPolicy(500000000, Set.of("video/mp4"),
            Duration.ofMinutes(15), Duration.ofHours(24), 10);

        assertThatThrownBy(() -> new CreateVideoUpload(videos, storage, transactions, policy, clock)
            .execute(UUID.randomUUID(), "../clip.mp4", "video/mp4", 10, null))
            .isInstanceOf(IllegalArgumentException.class);
        verify(storage, never()).signUpload(org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    }

    private Video pending() {
        return new Video(UUID.randomUUID(), UUID.randomUUID(), "users/u/videos/v/source", "clip.mp4",
            "video/mp4", 10, null, VideoStatus.PENDING, NOW.minusSeconds(60), NOW.minusSeconds(60),
            NOW.plusSeconds(3600), null, null);
    }
}
