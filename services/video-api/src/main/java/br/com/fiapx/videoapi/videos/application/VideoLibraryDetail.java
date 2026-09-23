package br.com.fiapx.videoapi.videos.application;

import java.time.Instant;
import java.util.UUID;

public record VideoLibraryDetail(
    String videoRef,
    String originalFilename,
    ProductVideoStatus status,
    Instant submittedAt,
    Instant uploadedAt,
    ProcessingView processing
) {
    public record ProcessingView(
        UUID jobId,
        ProductProcessingStatus status,
        Instant requestedAt,
        Instant startedAt,
        Instant finishedAt
    ) {
    }
}
