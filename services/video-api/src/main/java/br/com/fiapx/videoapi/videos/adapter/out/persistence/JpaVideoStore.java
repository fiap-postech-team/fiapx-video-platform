package br.com.fiapx.videoapi.videos.adapter.out.persistence;

import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import br.com.fiapx.videoapi.videos.domain.Video;
import br.com.fiapx.videoapi.videos.domain.VideoStatus;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public final class JpaVideoStore implements VideoStore {
    private final VideoRepository videos;

    public JpaVideoStore(VideoRepository videos) {
        this.videos = videos;
    }

    public Optional<Video> findConfirmed(UUID userId, String objectKey) {
        return videos.findByUserIdAndObjectKeyAndUploadStatus(userId, objectKey, VideoStatus.UPLOADED)
            .map(VideoEntity::toDomain);
    }

    public Optional<Video> findOwned(UUID userId, String objectKey) {
        return videos.findByUserIdAndObjectKey(userId, objectKey).map(VideoEntity::toDomain);
    }

    public Optional<Video> lockOwned(UUID userId, UUID videoId) {
        return videos.findByIdAndUserId(videoId, userId).map(VideoEntity::toDomain);
    }

    public Optional<Video> lock(UUID videoId) {
        return videos.lockById(videoId).map(VideoEntity::toDomain);
    }

    public List<UUID> pendingExpired(Instant now, int limit) {
        return videos.pendingExpired(now, limit);
    }

    public List<UUID> expiredUncleaned(int limit) {
        return videos.expiredUncleaned(limit);
    }

    public Video save(Video video) {
        return videos.save(new VideoEntity(video)).toDomain();
    }
}
