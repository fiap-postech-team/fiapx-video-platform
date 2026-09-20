package br.com.fiapx.videoapi.videos.application;

import br.com.fiapx.videoapi.videos.application.port.out.VideoObjectStorage;
import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import br.com.fiapx.videoapi.videos.application.port.out.VideoTransactions;
import br.com.fiapx.videoapi.videos.domain.Video;
import br.com.fiapx.videoapi.videos.domain.VideoStatus;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class ConfirmVideo {
    private final VideoStore videos;
    private final VideoObjectStorage storage;
    private final VideoTransactions transactions;
    private final Clock clock;

    public ConfirmVideo(VideoStore videos, VideoObjectStorage storage, VideoTransactions transactions, Clock clock) {
        this.videos = videos;
        this.storage = storage;
        this.transactions = transactions;
        this.clock = clock;
    }

    public Video execute(UUID userId, UUID videoId) {
        return transactions.execute(() -> confirmOwned(userId, videoId));
    }

    private Video confirmOwned(UUID userId, UUID videoId) {
        var video = videos.lockOwned(userId, videoId).orElseThrow(VideoUploadNotFoundException::new);
        if (video.status() == VideoStatus.UPLOADED) {
            return video;
        }
        if (video.status() != VideoStatus.PENDING || !clock.instant().isBefore(video.expiresAt())) {
            throw new VideoUploadConflictException();
        }
        var object = storage.stat(video.objectKey()).orElseThrow(VideoUploadConflictException::new);
        if (!matches(video, object)) {
            throw new VideoUploadConflictException();
        }
        return videos.save(video.confirm(clock.instant()));
    }

    private boolean matches(Video video, VideoObjectStorage.StoredObject object) {
        return video.sizeBytes() == object.sizeBytes()
            && video.contentType().equals(object.contentType())
            && (video.checksumSha256() == null
                || Objects.equals(video.checksumSha256(), object.checksumSha256()));
    }
}
