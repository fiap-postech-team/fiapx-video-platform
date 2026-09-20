package br.com.fiapx.videoapi.videos.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiapx.videoapi.videos.application.ExpireVideoUploads;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.video.cleanup-interval=PT24H")
@ActiveProfiles("local")
@Testcontainers(disabledWithoutDocker = true)
class VideoCleanupConcurrencyIT extends VideoUploadIntegrationSupport {
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ExpireVideoUploads cleanup;

    @Test
    void concurrentCleanersConvergeOnOneExpiredState() throws Exception {
        var owner = token();
        var upload = post("/v1/videos/uploads", Map.of("originalFilename", "clip.mp4",
            "contentType", "video/mp4", "sizeBytes", 10), owner).getBody();
        var id = UUID.fromString((String) upload.get("videoId"));
        jdbc.update("update videos set expires_at = now() - interval '1 minute' where id = ?", id);
        var start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> runAfter(start));
            var second = workers.submit(() -> runAfter(start));
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForObject("select upload_status from videos where id = ?", String.class, id))
            .isEqualTo("EXPIRED");
        assertThat(jdbc.queryForObject("select cleanup_completed_at is not null from videos where id = ?",
            Boolean.class, id)).isTrue();
    }

    private void runAfter(CountDownLatch start) {
        try {
            start.await();
            cleanup.run();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
