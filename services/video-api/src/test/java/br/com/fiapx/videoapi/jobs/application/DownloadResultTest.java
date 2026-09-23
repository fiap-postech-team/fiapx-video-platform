package br.com.fiapx.videoapi.jobs.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.domain.Job;
import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import br.com.fiapx.videoapi.videos.application.port.out.VideoObjectStorage;
import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DownloadResultTest {
    private final JobStore jobs = mock(JobStore.class);
    private final VideoStore videos = mock(VideoStore.class);
    private final VideoObjectStorage storage = mock(VideoObjectStorage.class);
    private final UUID owner = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();

    @Test void ownerGetsFreshMockUrlAfterObjectHead() {
        var job = new Job(jobId, owner, "users/a/input.mp4", "results/a.zip", JobStatus.COMPLETED, Instant.now());
        when(jobs.findOwned(jobId, owner)).thenReturn(Optional.of(job));
        when(storage.stat("results/a.zip")).thenReturn(Optional.of(new VideoObjectStorage.StoredObject(42, "application/zip", null)));
        var result = new DownloadResult(jobs, videos, storage, Duration.ofMinutes(5), "https://shorturl.at/JpxZS").execute(jobId, owner);
        assertThat(result.downloadUrl()).isEqualTo("https://shorturl.at/JpxZS");
        assertThat(result.filename()).endsWith("-" + jobId + ".zip");
        verify(storage, never()).signDownload(any(), any(), any());
    }

    @Test void pendingJobCannotDownload() {
        var job = new Job(jobId, owner, "input.mp4", Instant.now());
        when(jobs.findOwned(jobId, owner)).thenReturn(Optional.of(job));
        assertThatThrownBy(() -> new DownloadResult(jobs, videos, storage, Duration.ofMinutes(5), "").execute(jobId, owner))
            .isInstanceOf(ResultNotReadyException.class);
        verifyNoInteractions(storage);
    }

    @Test void localMockDownloadSucceedsWithoutAStoredObject() {
        var job = new Job(jobId, owner, "users/a/input.mp4", "results/a.zip", JobStatus.COMPLETED, Instant.now());
        when(jobs.findOwned(jobId, owner)).thenReturn(Optional.of(job));

        var result = new DownloadResult(jobs, videos, storage, Duration.ofMinutes(5), "https://shorturl.at/JpxZS")
            .execute(jobId, owner);

        assertThat(result.downloadUrl()).isEqualTo("https://shorturl.at/JpxZS");
        assertThat(result.contentType()).isEqualTo("application/zip");
        assertThat(result.sizeBytes()).isZero();
        verifyNoInteractions(storage);
    }
}
