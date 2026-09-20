package br.com.fiapx.videoapi.videos.domain;

import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record Video(UUID id, UUID userId, String objectKey, String originalFilename,
                    String contentType, long sizeBytes, String checksumSha256,
                    VideoStatus status, Instant createdAt, Instant updatedAt,
                    Instant expiresAt, Instant uploadedAt, Instant cleanupCompletedAt) {
    public Video(UUID id, UUID userId, String objectKey, String originalFilename,
                 String contentType, long sizeBytes, String checksumSha256,
                 VideoStatus status, Instant createdAt, Instant updatedAt) {
        this(id, userId, objectKey, originalFilename, contentType, sizeBytes, checksumSha256,
            status, createdAt, updatedAt, createdAt.plus(Duration.ofHours(24)),
            status == VideoStatus.UPLOADED ? updatedAt : null, null);
    }

    public boolean isConfirmed() {
        return status == VideoStatus.UPLOADED;
    }

    public Video confirm(Instant now) {
        if (status == VideoStatus.UPLOADED) {
            return this;
        }
        if (status != VideoStatus.PENDING) {
            throw new IllegalStateException("Video cannot be confirmed");
        }
        if (!now.isBefore(expiresAt)) {
            throw new IllegalStateException("Video upload expired");
        }
        var confirmedAt = now.truncatedTo(ChronoUnit.MICROS);
        return new Video(id, userId, objectKey, originalFilename, contentType, sizeBytes,
            checksumSha256, VideoStatus.UPLOADED, createdAt, confirmedAt, expiresAt, confirmedAt, null);
    }

    public Video expire(Instant now) {
        if (status != VideoStatus.PENDING || now.isBefore(expiresAt)) {
            throw new IllegalStateException("Video cannot expire");
        }
        return new Video(id, userId, objectKey, originalFilename, contentType, sizeBytes,
            checksumSha256, VideoStatus.EXPIRED, createdAt, now, expiresAt, null, null);
    }

    public Video cleaned(Instant now) {
        if (status != VideoStatus.EXPIRED) {
            throw new IllegalStateException("Only expired uploads can be cleaned");
        }
        return new Video(id, userId, objectKey, originalFilename, contentType, sizeBytes,
            checksumSha256, status, createdAt, updatedAt, expiresAt, uploadedAt, now);
    }
}
