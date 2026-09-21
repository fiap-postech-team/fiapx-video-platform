package br.com.fiapx.videoapi.videos.application.port.out;

import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import br.com.fiapx.videoapi.videos.domain.VideoStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VideoLibraryReader {
    Page findPage(UUID ownerId, int pageNumber, int pageSize);

    Optional<DetailRow> findDetail(UUID ownerId, UUID videoId);

    record Page(List<Row> items, long totalItems) {
    }

    record Row(
        UUID videoId,
        String originalFilename,
        VideoStatus uploadStatus,
        Instant submittedAt,
        JobStatus jobStatus,
        Instant jobCreatedAt,
        Instant jobUpdatedAt
    ) {
    }

    record DetailRow(
        UUID videoId,
        String originalFilename,
        VideoStatus uploadStatus,
        Instant submittedAt,
        Instant uploadedAt,
        UUID jobId,
        JobStatus jobStatus,
        Instant jobCreatedAt,
        Instant startedAt,
        Instant finishedAt
    ) {
    }
}
