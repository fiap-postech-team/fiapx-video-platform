package br.com.fiapx.videoapi.videos.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers(disabledWithoutDocker = true)
class VideoLibraryIT extends VideoUploadIntegrationSupport {
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void listsOwnedVideosWithoutInternalFieldsAndHidesOtherOwners() throws Exception {
        var owner = token();
        var other = token();
        var ownerId = UUID.fromString(me(owner).get("id").toString());
        var pending = insertVideo(ownerId, "pendente.mp4", "PENDING", Instant.parse("2026-09-20T10:00:00Z"), null);
        var processed = insertVideo(ownerId, "pronto.mp4", "UPLOADED", Instant.parse("2026-09-20T09:00:00Z"),
            Instant.parse("2026-09-20T09:01:00Z"));
        var jobId = UUID.randomUUID();
        insertJob(jobId, ownerId, processed, Instant.parse("2026-09-20T12:00:00Z"), "COMPLETED");
        insertHistory(jobId, "PENDING", Instant.parse("2026-09-20T12:00:00Z"));
        insertHistory(jobId, "PROCESSING", Instant.parse("2026-09-20T12:00:05Z"));
        insertHistory(jobId, "COMPLETED", Instant.parse("2026-09-20T12:16:42Z"));
        insertVideo(UUID.fromString(me(other).get("id").toString()), "secreto.mp4", "UPLOADED",
            Instant.parse("2026-09-20T13:00:00Z"), Instant.parse("2026-09-20T13:01:00Z"));

        var page = get("/v1/videos?page=1", owner);
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = page.getBody();
        assertThat(body.get("pageSize")).isEqualTo(5);
        var items = (java.util.List<Map<String, Object>>) body.get("items");
        assertThat(items).extracting(item -> item.get("originalFilename"))
            .containsExactly("pronto.mp4", "pendente.mp4");
        assertThat(items.getFirst().get("status")).isEqualTo("AVAILABLE");
        assertThat(items.getLast().get("status")).isEqualTo("AWAITING_UPLOAD");
        assertThat(items.getFirst().keySet()).doesNotContain("sourceKey", "resultKey", "userId", "id");
        assertThat(get("/v1/videos?page=1", other).getBody().get("totalItems")).isEqualTo(1);

        var videoRef = items.getFirst().get("videoRef").toString();
        var detail = get("/v1/videos/" + videoRef, owner);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().get("originalFilename")).isEqualTo("pronto.mp4");
        var processing = (Map<String, Object>) detail.getBody().get("processing");
        assertThat(processing.get("jobId")).isEqualTo(jobId.toString());
        assertThat(processing.get("status")).isEqualTo("AVAILABLE");
        assertThat(processing.get("startedAt")).asString().contains("12:00:05");
        assertThat(get("/v1/videos/" + videoRef, other).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/v1/videos/not-a-ref", owner).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void paginatesFiveItemsAndReturnsEmptyBeyondTheEnd() {
        var owner = token();
        var ownerId = UUID.fromString(me(owner).get("id").toString());
        var start = Instant.parse("2026-09-01T00:00:00Z");
        for (int index = 0; index < 11; index++) {
            insertVideo(ownerId, "aula-" + index + ".mp4", "UPLOADED", start.plusSeconds(index), start.plusSeconds(index));
        }
        var first = get("/v1/videos?page=1", owner).getBody();
        var second = get("/v1/videos?page=2", owner).getBody();
        var third = get("/v1/videos?page=3", owner).getBody();
        var fourth = get("/v1/videos?page=4", owner).getBody();
        assertThat((java.util.List<?>) first.get("items")).hasSize(5);
        assertThat(first.get("pageSize")).isEqualTo(5);
        assertThat(first.get("totalItems")).isEqualTo(11);
        assertThat(first.get("totalPages")).isEqualTo(3);
        assertThat((java.util.List<?>) second.get("items")).hasSize(5);
        assertThat((java.util.List<?>) third.get("items")).hasSize(1);
        assertThat((java.util.List<?>) fourth.get("items")).isEmpty();
        assertThat(fourth.get("totalItems")).isEqualTo(11);
        assertThat(get("/v1/videos?page=0", owner).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(get("/v1/videos", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsASecondJobForTheSameConfirmedVideo() throws Exception {
        var owner = token();
        var bytes = "library job fixture".getBytes(StandardCharsets.UTF_8);
        var upload = post("/v1/videos/uploads", Map.of("originalFilename", "clip.mp4",
            "contentType", "video/mp4", "sizeBytes", bytes.length), owner).getBody();
        assertThat(put(upload, bytes)).isEqualTo(200);
        assertThat(post("/v1/videos/" + upload.get("videoId") + "/confirm", null, owner)
            .getStatusCode()).isEqualTo(HttpStatus.OK);
        var first = post("/v1/jobs", Map.of("sourceKey", upload.get("sourceKey")), owner);
        var second = post("/v1/jobs", Map.of("sourceKey", upload.get("sourceKey")), owner);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody().get("code")).isEqualTo("PROCESSING_ALREADY_EXISTS");
    }

    @Test
    void concurrentCreatesLeaveASingleVisibleJob() throws Exception {
        var owner = token();
        var bytes = "race fixture".getBytes(StandardCharsets.UTF_8);
        var upload = post("/v1/videos/uploads", Map.of("originalFilename", "race.mp4",
            "contentType", "video/mp4", "sizeBytes", bytes.length), owner).getBody();
        assertThat(put(upload, bytes)).isEqualTo(200);
        post("/v1/videos/" + upload.get("videoId") + "/confirm", null, owner);
        var pool = Executors.newFixedThreadPool(2);
        var one = pool.submit(() -> post("/v1/jobs", Map.of("sourceKey", upload.get("sourceKey")), owner).getStatusCode());
        var two = pool.submit(() -> post("/v1/jobs", Map.of("sourceKey", upload.get("sourceKey")), owner).getStatusCode());
        pool.shutdown();
        pool.awaitTermination(15, TimeUnit.SECONDS);
        assertThat(java.util.List.of(one.get(), two.get()))
            .containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
    }

    private Map<String, Object> me(String token) {
        return get("/v1/me", token).getBody();
    }

    private UUID insertVideo(UUID ownerId, String name, String status, Instant created, Instant uploaded) {
        var id = UUID.randomUUID();
        jdbc.update("""
            insert into videos (id, user_id, object_key, original_filename, declared_content_type, size_bytes,
                upload_status, created_at, updated_at, expires_at, uploaded_at)
            values (?, ?, ?, ?, 'video/mp4', 1, ?, ?, ?, ?, ?)
            """, id, ownerId, "users/" + ownerId + "/" + id, name, status, ts(created), ts(created),
            ts(created.plusSeconds(86_400)), ts(uploaded));
        return id;
    }

    private void insertJob(UUID jobId, UUID ownerId, UUID videoId, Instant at, String status) {
        jdbc.update("""
            insert into jobs (id, user_id, video_id, source_kind, source_key, status, created_at, updated_at,
                version, video_library_visible)
            values (?, ?, ?, 'VIDEO', ?, ?, ?, ?, 0, true)
            """, jobId, ownerId, videoId, "users/" + ownerId + "/" + videoId + "/source", status, ts(at), ts(at));
    }

    private void insertHistory(UUID jobId, String status, Instant at) {
        jdbc.update("insert into job_status_history (job_id, status, occurred_at) values (?, ?, ?)",
            jobId, status, ts(at));
    }

    private static Timestamp ts(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
