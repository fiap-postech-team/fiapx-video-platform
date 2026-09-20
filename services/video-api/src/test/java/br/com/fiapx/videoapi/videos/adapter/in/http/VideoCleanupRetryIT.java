package br.com.fiapx.videoapi.videos.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiapx.videoapi.videos.application.ExpireVideoUploads;
import br.com.fiapx.videoapi.videos.application.StorageUnavailableException;
import br.com.fiapx.videoapi.videos.application.VideoUploadPolicy;
import br.com.fiapx.videoapi.videos.application.port.out.VideoObjectStorage;
import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import br.com.fiapx.videoapi.videos.application.port.out.VideoTransactions;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.video.cleanup-interval=PT23H")
@ActiveProfiles("local")
@Testcontainers(disabledWithoutDocker = true)
class VideoCleanupRetryIT extends VideoUploadIntegrationSupport {
    @Autowired JdbcTemplate jdbc;
    @Autowired VideoStore videos;
    @Autowired VideoTransactions transactions;
    @Autowired VideoUploadPolicy policy;
    @Autowired VideoObjectStorage objectStorage;
    @Autowired Clock clock;

    @Test
    void retriesDeletionAfterTemporaryStorageFailure() throws Exception {
        var owner = token();
        var upload = post("/v1/videos/uploads", Map.of("originalFilename", "clip.mp4",
            "contentType", "video/mp4", "sizeBytes", 8), owner).getBody();
        var id = UUID.fromString((String) upload.get("videoId"));
        assertThat(put(upload, "12345678".getBytes())).isEqualTo(200);
        jdbc.update("update videos set expires_at = now() - interval '1 minute' where id = ?", id);
        var cleanup = new ExpireVideoUploads(videos, failOnce(), transactions, policy, clock);

        cleanup.run();
        assertThat(jdbc.queryForObject("select cleanup_completed_at is null from videos where id = ?",
            Boolean.class, id)).isTrue();
        assertThat(objectStorage.stat((String) upload.get("sourceKey"))).isPresent();

        cleanup.run();
        assertThat(jdbc.queryForObject("select cleanup_completed_at is not null from videos where id = ?",
            Boolean.class, id)).isTrue();
        assertThat(objectStorage.stat((String) upload.get("sourceKey"))).isEmpty();
    }

    private VideoObjectStorage failOnce() {
        return new VideoObjectStorage() {
            private int attempts;
            public SignedUpload signUpload(String key, String type, String checksum, Duration ttl) {
                return objectStorage.signUpload(key, type, checksum, ttl);
            }
            public Optional<StoredObject> stat(String key) { return objectStorage.stat(key); }
            public void delete(String key) {
                if (++attempts == 1) {
                    throw new StorageUnavailableException(new IOException());
                }
                objectStorage.delete(key);
            }
        };
    }
}
