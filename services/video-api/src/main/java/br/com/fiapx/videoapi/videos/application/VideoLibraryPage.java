package br.com.fiapx.videoapi.videos.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VideoLibraryPage(
    List<VideoLibraryItem> items,
    int page,
    int pageSize,
    long totalItems,
    int totalPages
) {
    public record VideoLibraryItem(
        String videoRef,
        String originalFilename,
        ProductVideoStatus status,
        UUID jobId,
        Instant submittedAt,
        Instant activityAt
    ) {
    }
}
