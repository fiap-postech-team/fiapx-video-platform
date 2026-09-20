package br.com.fiapx.videoapi.videos.adapter.out.persistence;

import br.com.fiapx.videoapi.videos.domain.Video;
import br.com.fiapx.videoapi.videos.domain.VideoStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "videos")
class VideoEntity {
    @Id UUID id;
    UUID userId;
    String objectKey;
    String originalFilename;
    String declaredContentType;
    long sizeBytes;
    String checksumSha256;
    @Enumerated(EnumType.STRING) VideoStatus uploadStatus;
    Instant createdAt;
    Instant updatedAt;
    Instant expiresAt;
    Instant uploadedAt;
    Instant cleanupCompletedAt;

    protected VideoEntity() {
    }

    VideoEntity(Video video) {
        id = video.id(); userId = video.userId(); objectKey = video.objectKey();
        originalFilename = video.originalFilename(); declaredContentType = video.contentType();
        sizeBytes = video.sizeBytes(); checksumSha256 = video.checksumSha256();
        uploadStatus = video.status(); createdAt = video.createdAt(); updatedAt = video.updatedAt();
        expiresAt = video.expiresAt(); uploadedAt = video.uploadedAt();
        cleanupCompletedAt = video.cleanupCompletedAt();
    }

    Video toDomain() {
        return new Video(id, userId, objectKey, originalFilename, declaredContentType, sizeBytes,
            checksumSha256, uploadStatus, createdAt, updatedAt, expiresAt, uploadedAt, cleanupCompletedAt);
    }
}
