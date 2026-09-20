package br.com.fiapx.videoapi.videos.application;

import java.time.Duration;
import java.util.Set;

public record VideoUploadPolicy(long maxSizeBytes, Set<String> allowedContentTypes,
                                Duration uploadUrlTtl, Duration pendingTtl, int cleanupBatchSize) {
    public VideoUploadPolicy {
        if (maxSizeBytes < 1 || allowedContentTypes == null || allowedContentTypes.isEmpty()
            || uploadUrlTtl.isNegative() || uploadUrlTtl.isZero()
            || pendingTtl.compareTo(uploadUrlTtl) <= 0 || cleanupBatchSize < 1) {
            throw new IllegalArgumentException("Invalid video upload policy");
        }
        allowedContentTypes = Set.copyOf(allowedContentTypes);
    }

    public void validate(String filename, String contentType, long sizeBytes, String checksumSha256) {
        if (filename == null || filename.isBlank() || filename.length() > 512
            || filename.chars().anyMatch(c -> c < 32 || c == '/' || c == '\\')) {
            throw new IllegalArgumentException("Invalid original filename");
        }
        if (!allowedContentTypes.contains(contentType) || sizeBytes < 1 || sizeBytes > maxSizeBytes) {
            throw new IllegalArgumentException("Invalid declared video metadata");
        }
        if (checksumSha256 != null && !checksumSha256.matches("(?i)[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Invalid SHA-256 checksum");
        }
    }
}
