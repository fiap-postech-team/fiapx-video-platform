package br.com.fiapx.videoapi.videos.domain;

import java.time.Instant;
import java.util.UUID;

public record Video(UUID id, UUID userId, String objectKey, String originalFilename,
                    String contentType, long sizeBytes, String checksumSha256,
                    VideoStatus status, Instant createdAt, Instant updatedAt) {
    public boolean isConfirmed() {
        return status == VideoStatus.UPLOADED;
    }

    public Video confirm(Instant now) {
        if (status != VideoStatus.PENDING) {
            throw new IllegalStateException("Video cannot be confirmed");
        }
        return new Video(id, userId, objectKey, originalFilename, contentType, sizeBytes,
            checksumSha256, VideoStatus.UPLOADED, createdAt, now);
    }
}
