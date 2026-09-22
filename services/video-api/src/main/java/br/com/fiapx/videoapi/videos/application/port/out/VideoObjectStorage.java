package br.com.fiapx.videoapi.videos.application.port.out;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public interface VideoObjectStorage {
    SignedUpload signUpload(String key, String contentType, String checksumSha256, Duration ttl);
    default SignedDownload signDownload(String key, String filename, Duration ttl) {
        throw new UnsupportedOperationException("Download signing is not available");
    }
    Optional<StoredObject> stat(String key);
    void delete(String key);

    record SignedUpload(String url, Instant expiresAt, Map<String, String> headers) { }
    record SignedDownload(String url, Instant expiresAt) { }
    record StoredObject(long sizeBytes, String contentType, String checksumSha256) { }
}
