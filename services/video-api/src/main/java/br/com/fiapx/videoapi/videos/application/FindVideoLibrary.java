package br.com.fiapx.videoapi.videos.application;

import br.com.fiapx.videoapi.videos.application.port.out.VideoLibraryReader;
import java.time.Instant;
import java.util.UUID;

public final class FindVideoLibrary {
    static final int PAGE_SIZE = 5;
    private final VideoLibraryReader reader;

    public FindVideoLibrary(VideoLibraryReader reader) {
        this.reader = reader;
    }

    public VideoLibraryPage execute(UUID ownerId, int pageNumber) {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("Invalid page");
        }
        var result = reader.findPage(ownerId, pageNumber, PAGE_SIZE);
        var totalPages = result.totalItems() == 0 ? 0 : (int) Math.ceil(result.totalItems() / (double) PAGE_SIZE);
        var items = result.items().stream().map(row -> new VideoLibraryPage.VideoLibraryItem(
            VideoRef.encode(row.videoId()),
            row.originalFilename(),
            ProductStatusMapper.videoStatus(row.uploadStatus(), row.jobStatus()),
            row.submittedAt(),
            activityAt(row)
        )).toList();
        return new VideoLibraryPage(items, pageNumber, PAGE_SIZE, result.totalItems(), totalPages);
    }

    private static Instant activityAt(VideoLibraryReader.Row row) {
        if (row.jobUpdatedAt() != null) {
            return row.jobUpdatedAt();
        }
        if (row.jobCreatedAt() != null) {
            return row.jobCreatedAt();
        }
        return row.submittedAt();
    }
}
