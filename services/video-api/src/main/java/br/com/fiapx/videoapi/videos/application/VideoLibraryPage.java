package br.com.fiapx.videoapi.videos.application;

import java.time.Instant;
import java.util.List;

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
        Instant submittedAt,
        Instant activityAt
    ) {
    }
}
