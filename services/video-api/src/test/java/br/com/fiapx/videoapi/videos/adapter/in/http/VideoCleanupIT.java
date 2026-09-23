package br.com.fiapx.videoapi.videos.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiapx.videoapi.videos.application.ExpireVideoUploads;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.model.S3Exception;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.video.cleanup-interval=PT24H")
@ActiveProfiles("local")
@Testcontainers(disabledWithoutDocker = true)
class VideoCleanupIT extends VideoUploadIntegrationSupport {
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ExpireVideoUploads cleanup;

    @Test
    void expiresAbandonedUploadAndDeletesItsObject() throws Exception {
        var owner = token();
        var bytes = "orphan video".getBytes(StandardCharsets.UTF_8);
        var upload = newUpload(owner, bytes.length);
        var videoId = UUID.fromString((String) upload.get("videoId"));
        assertThat(put(upload, bytes)).isEqualTo(200);
        jdbc.update("update videos set expires_at = now() - interval '1 minute' where id = ?", videoId);

        cleanup.run();

        assertThat(jdbc.queryForObject("select upload_status from videos where id = ?", String.class, videoId))
            .isEqualTo("EXPIRED");
        assertThat(jdbc.queryForObject("select cleanup_completed_at is not null from videos where id = ?",
            Boolean.class, videoId)).isTrue();
        assertThatThrownBy(() -> storage.headObject(request -> request.bucket("videos")
            .key((String) upload.get("sourceKey")))).isInstanceOf(S3Exception.class)
            .satisfies(error -> assertThat(((S3Exception) error).statusCode()).isEqualTo(404));
        assertThat(post(confirmPath(upload), null, owner).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(post("/v1/jobs", Map.of("sourceKey", upload.get("sourceKey")), owner)
            .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void neverDeletesConfirmedVideo() throws Exception {
        var owner = token();
        var bytes = "confirmed video".getBytes(StandardCharsets.UTF_8);
        var upload = newUpload(owner, bytes.length);
        var videoId = UUID.fromString((String) upload.get("videoId"));
        assertThat(put(upload, bytes)).isEqualTo(200);
        assertThat(post(confirmPath(upload), null, owner).getStatusCode()).isEqualTo(HttpStatus.OK);
        jdbc.update("update videos set expires_at = now() - interval '1 minute' where id = ?", videoId);

        cleanup.run();

        assertThat(jdbc.queryForObject("select upload_status from videos where id = ?", String.class, videoId))
            .isEqualTo("UPLOADED");
        assertThat(storage.headObject(request -> request.bucket("videos")
            .key((String) upload.get("sourceKey"))).contentLength()).isEqualTo(bytes.length);
    }

    private Map<String, Object> newUpload(String owner, int size) {
        return post("/v1/videos/uploads", Map.of("originalFilename", "clip.mp4",
            "contentType", "video/mp4", "sizeBytes", size), owner).getBody();
    }

    private String confirmPath(Map<String, Object> upload) {
        return "/v1/videos/" + upload.get("videoId") + "/confirm";
    }
}
