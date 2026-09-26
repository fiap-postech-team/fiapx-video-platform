package br.com.fiapx.videoapi.videos.application;

import br.com.fiapx.videoapi.videos.application.port.out.VideoObjectStorage;
import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import br.com.fiapx.videoapi.videos.application.port.out.VideoTransactions;
import br.com.fiapx.videoapi.videos.domain.VideoStatus;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.fiapx.videoapi.foundation.observability.BusinessMetrics;

public final class ExpireVideoUploads {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpireVideoUploads.class);
    private final VideoStore videos;
    private final VideoObjectStorage storage;
    private final VideoTransactions transactions;
    private final VideoUploadPolicy policy;
    private final Clock clock;
    private final BusinessMetrics metrics;

    public ExpireVideoUploads(VideoStore videos, VideoObjectStorage storage, VideoTransactions transactions,
                              VideoUploadPolicy policy, Clock clock) {
        this(videos, storage, transactions, policy, clock, null);
    }

    public ExpireVideoUploads(VideoStore videos, VideoObjectStorage storage, VideoTransactions transactions,
                              VideoUploadPolicy policy, Clock clock, BusinessMetrics metrics) {
        this.videos = videos;
        this.storage = storage;
        this.transactions = transactions;
        this.policy = policy;
        this.clock = clock;
        this.metrics = metrics;
    }

    public void run() {
        for (var videoId : videos.pendingExpired(clock.instant(), policy.cleanupBatchSize())) {
            var expired = transactions.execute(() -> expireIfDue(videoId));
            if (expired && metrics != null) metrics.uploadExpired();
        }
        for (var videoId : videos.expiredUncleaned(policy.cleanupBatchSize())) {
            cleanIfExpired(videoId);
        }
    }

    private boolean expireIfDue(UUID videoId) {
        var pending = videos.lock(videoId).filter(video -> video.status() == VideoStatus.PENDING)
            .filter(video -> !clock.instant().isBefore(video.expiresAt()));
        if (pending.isEmpty()) return false;
        videos.save(pending.get().expire(clock.instant()));
        return true;
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
