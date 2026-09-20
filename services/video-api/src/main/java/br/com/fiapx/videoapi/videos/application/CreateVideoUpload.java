package br.com.fiapx.videoapi.videos.application;

import br.com.fiapx.videoapi.videos.application.port.out.VideoObjectStorage;
import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import br.com.fiapx.videoapi.videos.application.port.out.VideoTransactions;
import br.com.fiapx.videoapi.videos.domain.Video;
import br.com.fiapx.videoapi.videos.domain.VideoStatus;
import java.time.Clock;
import java.util.Locale;
import java.util.UUID;

public final class CreateVideoUpload {
    private final VideoStore videos;
    private final VideoObjectStorage storage;
    private final VideoTransactions transactions;
    private final VideoUploadPolicy policy;
    private final Clock clock;

    public CreateVideoUpload(VideoStore videos, VideoObjectStorage storage, VideoTransactions transactions,
                             VideoUploadPolicy policy, Clock clock) {
        this.videos = videos;
        this.storage = storage;
        this.transactions = transactions;
        this.policy = policy;
        this.clock = clock;
    }

    public CreatedUpload execute(UUID userId, String filename, String contentType,
                                 long sizeBytes, String checksumSha256) {
        policy.validate(filename, contentType, sizeBytes, checksumSha256);
        var now = clock.instant();
        var videoId = UUID.randomUUID();
        var key = "users/" + userId + "/videos/" + videoId + "/source";
        var checksum = checksumSha256 == null ? null : checksumSha256.toLowerCase(Locale.ROOT);
        var signed = storage.signUpload(key, contentType, checksum, policy.uploadUrlTtl());
        var video = new Video(videoId, userId, key, filename, contentType, sizeBytes, checksum,
            VideoStatus.PENDING, now, now, now.plus(policy.pendingTtl()), null, null);
        transactions.execute(() -> videos.save(video));
        return new CreatedUpload(videoId, key, signed);
    }

    public record CreatedUpload(UUID videoId, String sourceKey, VideoObjectStorage.SignedUpload signed) { }
}
