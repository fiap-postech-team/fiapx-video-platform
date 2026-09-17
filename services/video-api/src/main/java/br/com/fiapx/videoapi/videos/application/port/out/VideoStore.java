package br.com.fiapx.videoapi.videos.application.port.out;

import br.com.fiapx.videoapi.videos.domain.Video;
import java.util.Optional;
import java.util.UUID;

public interface VideoStore {
    Optional<Video> findConfirmed(UUID userId, String objectKey);
    default Optional<Video> findOwned(UUID userId, String objectKey) {
        return findConfirmed(userId, objectKey);
    }
    Video save(Video video);
}
