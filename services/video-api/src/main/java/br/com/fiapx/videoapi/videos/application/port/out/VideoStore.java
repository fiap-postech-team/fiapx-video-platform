package br.com.fiapx.videoapi.videos.application.port.out;

import br.com.fiapx.videoapi.videos.domain.Video;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.util.List;

public interface VideoStore {
    Optional<Video> findConfirmed(UUID userId, String objectKey);
    default Optional<Video> findOwned(UUID userId, String objectKey) {
        return findConfirmed(userId, objectKey);
    }
    default Optional<Video> lockOwned(UUID userId, UUID videoId) {
        throw new UnsupportedOperationException();
    }
    default Optional<Video> lock(UUID videoId) {
        throw new UnsupportedOperationException();
    }
    default Optional<Video> findByJobOwner(UUID videoId, UUID userId) {
        return lockOwned(userId, videoId);
    }
    default List<UUID> pendingExpired(Instant now, int limit) {
        throw new UnsupportedOperationException();
    }
    default List<UUID> expiredUncleaned(int limit) {
        throw new UnsupportedOperationException();
    }
    default List<Video> findOwnedPage(UUID userId, int offset, int limit) {
        throw new UnsupportedOperationException();
    }
    default long countOwned(UUID userId) {
        throw new UnsupportedOperationException();
    }
    default Optional<Video> findOwnedById(UUID userId, UUID videoId) {
        return Optional.empty();
    }
    Video save(Video video);
}
