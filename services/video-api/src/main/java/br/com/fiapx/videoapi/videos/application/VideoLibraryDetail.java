package br.com.fiapx.videoapi.videos.application;

import java.time.Instant;

public record VideoLibraryDetail(
    String videoRef,
    String originalFilename,
    ProductVideoStatus status,
    Instant submittedAt,
    Instant uploadedAt,
    ProcessingView processing
) {
    public record ProcessingView(
        ProductProcessingStatus status,
        Instant requestedAt,
        Instant startedAt,
        Instant finishedAt
    ) {
    }
}
