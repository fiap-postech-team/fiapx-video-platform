package br.com.fiapx.videoapi.videos.application;

import br.com.fiapx.videoapi.videos.application.port.out.VideoObjectStorage;
import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import br.com.fiapx.videoapi.videos.application.port.out.VideoTransactions;
import br.com.fiapx.videoapi.videos.domain.VideoStatus;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExpireVideoUploads {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpireVideoUploads.class);
    private final VideoStore videos;
    private final VideoObjectStorage storage;
    private final VideoTransactions transactions;
    private final VideoUploadPolicy policy;
    private final Clock clock;

    public ExpireVideoUploads(VideoStore videos, VideoObjectStorage storage, VideoTransactions transactions,
                              VideoUploadPolicy policy, Clock clock) {
        this.videos = videos;
        this.storage = storage;
        this.transactions = transactions;
        this.policy = policy;
        this.clock = clock;
    }

    public void run() {
        for (var videoId : videos.pendingExpired(clock.instant(), policy.cleanupBatchSize())) {
            transactions.execute(() -> expireIfDue(videoId));
        }
        for (var videoId : videos.expiredUncleaned(policy.cleanupBatchSize())) {
            cleanIfExpired(videoId);
        }
    }

    private Void expireIfDue(UUID videoId) {
        videos.lock(videoId).filter(video -> video.status() == VideoStatus.PENDING)
            .filter(video -> !clock.instant().isBefore(video.expiresAt()))
            .ifPresent(video -> videos.save(video.expire(clock.instant())));
        return null;
    }

    private void cleanIfExpired(UUID videoId) {
        var expired = transactions.execute(() -> videos.lock(videoId)
            .filter(video -> video.status() == VideoStatus.EXPIRED)
            .filter(video -> video.cleanupCompletedAt() == null));
        if (expired.isEmpty()) {
            return;
        }
        try {
            storage.delete(expired.get().objectKey());
            transactions.execute(() -> {
                videos.lock(videoId).filter(video -> video.status() == VideoStatus.EXPIRED)
                    .filter(video -> video.cleanupCompletedAt() == null)
                    .ifPresent(video -> videos.save(video.cleaned(clock.instant())));
                return null;
            });
        } catch (StorageUnavailableException exception) {
            LOGGER.warn("Expired video cleanup deferred videoId={}", videoId);
        }
    }
}
