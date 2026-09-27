package br.com.fiapx.videoapi.videos.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URLEncoder;
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
import org.springframework.http.ResponseEntity;
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
    void searchesNamesByPrefixOrExactValueAndTreatsWildcardsAsText() {
        var owner = token();
        var ownerId = UUID.fromString(me(owner).get("id").toString());
        var start = Instant.parse("2026-09-20T10:00:00Z");
        insertVideo(ownerId, "Black-1.mp4", "UPLOADED", start, start);
        insertVideo(ownerId, "Blackstock.mp4", "UPLOADED", start.plusSeconds(1), start.plusSeconds(1));
        insertVideo(ownerId, "Black%literal.mp4", "UPLOADED", start.plusSeconds(2), start.plusSeconds(2));
        insertVideo(ownerId, "Black_literal.mp4", "UPLOADED", start.plusSeconds(3), start.plusSeconds(3));
        insertVideo(ownerId, "Black\\literal.mp4", "UPLOADED", start.plusSeconds(4), start.plusSeconds(4));
        insertVideo(ownerId, "Aula.mp4", "UPLOADED", start.plusSeconds(5), start.plusSeconds(5));

        var prefix = get("/v1/videos?name=BLA&match=PREFIX", owner).getBody();
        assertThat(prefix.get("totalItems")).isEqualTo(5);
        assertThat(names(prefix)).containsExactly(
            "Black\\literal.mp4", "Black_literal.mp4", "Black%literal.mp4", "Blackstock.mp4", "Black-1.mp4");

        var exact = get("/v1/videos?name=BLACKSTOCK.MP4&match=EXACT", owner).getBody();
        assertThat(names(exact)).containsExactly("Blackstock.mp4");
        assertThat(get("/v1/videos?name=" + encoded("  bla  "), owner).getBody().get("totalItems"))
            .isEqualTo(5);
        assertThat(names(get("/v1/videos?name=" + encoded("Black%"), owner).getBody()))
            .containsExactly("Black%literal.mp4");
        assertThat(names(get("/v1/videos?name=" + encoded("Black_"), owner).getBody()))
            .containsExactly("Black_literal.mp4");
        assertThat(names(get("/v1/videos?name=" + encoded("Black\\"), owner).getBody()))
            .containsExactly("Black\\literal.mp4");
        assertThat(get("/v1/videos?name=" + encoded("   "), owner).getBody().get("totalItems"))
            .isEqualTo(6);
    }

    @Test
    void groupsStatusesAndCombinesThemBeforePagination() {
        var owner = token();
        var ownerId = UUID.fromString(me(owner).get("id").toString());
        var start = Instant.parse("2026-09-21T10:00:00Z");
        insertVideo(ownerId, "awaiting.mp4", "PENDING", start, null);
        insertVideo(ownerId, "uploaded.mp4", "UPLOADED", start.plusSeconds(1), start.plusSeconds(1));
        var queued = insertVideo(ownerId, "queued.mp4", "UPLOADED", start.plusSeconds(2), start.plusSeconds(2));
        var active = insertVideo(ownerId, "active.mp4", "UPLOADED", start.plusSeconds(3), start.plusSeconds(3));
        var completed = insertVideo(ownerId, "processed.mp4", "UPLOADED", start.plusSeconds(4), start.plusSeconds(4));
        var failed = insertVideo(ownerId, "failed.mp4", "UPLOADED", start.plusSeconds(5), start.plusSeconds(5));
        insertJob(UUID.randomUUID(), ownerId, queued, start.plusSeconds(6), "PENDING");
        insertJob(UUID.randomUUID(), ownerId, active, start.plusSeconds(7), "PROCESSING");
        insertJob(UUID.randomUUID(), ownerId, completed, start.plusSeconds(8), "COMPLETED");
        insertJob(UUID.randomUUID(), ownerId, failed, start.plusSeconds(9), "FAILED");
        insertVideo(ownerId, "rejected.mp4", "REJECTED", start.plusSeconds(10), null);
        insertVideo(ownerId, "expired.mp4", "EXPIRED", start.plusSeconds(11), null);

        assertThat(names(get("/v1/videos?status=PROCESSED", owner).getBody()))
            .containsExactly("processed.mp4");
        assertThat(names(get("/v1/videos?status=PROCESSING", owner).getBody()))
            .containsExactly("active.mp4", "queued.mp4", "uploaded.mp4");
        assertThat(names(get("/v1/videos?status=FAILED", owner).getBody()))
            .containsExactly("expired.mp4", "rejected.mp4", "failed.mp4");
        assertThat(names(get("/v1/videos?name=pro&status=PROCESSED", owner).getBody()))
            .containsExactly("processed.mp4");

        assertThat(names(get("/v1/videos?sort=STATUS&direction=ASC", owner).getBody()))
            .containsExactly("expired.mp4", "failed.mp4", "awaiting.mp4", "processed.mp4", "active.mp4");
        assertThat(names(get("/v1/videos?sort=UPDATED_AT&direction=ASC", owner).getBody()))
            .containsExactly("awaiting.mp4", "uploaded.mp4", "queued.mp4", "active.mp4", "processed.mp4");

        for (int index = 0; index < 6; index++) {
            insertVideo(ownerId, "batch-" + index + ".mp4", "UPLOADED",
                start.plusSeconds(20 + index), start.plusSeconds(20 + index));
        }
        var first = get("/v1/videos?page=1&name=batch&status=PROCESSING", owner).getBody();
        var second = get("/v1/videos?page=2&name=batch&status=PROCESSING", owner).getBody();
        assertThat(first.get("totalItems")).isEqualTo(6);
        assertThat(first.get("totalPages")).isEqualTo(2);
        assertThat(names(first)).hasSize(5);
        assertThat(names(second)).containsExactly("batch-0.mp4");
    }

    @Test
    void rejectsInvalidLibraryCriteria() {
        var owner = token();

        assertValidationError(get("/v1/videos?status=UNKNOWN", owner));
        assertValidationError(get("/v1/videos?match=CONTAINS", owner));
        assertValidationError(get("/v1/videos?sort=NAME", owner));
        assertValidationError(get("/v1/videos?direction=SIDEWAYS", owner));
        assertValidationError(get("/v1/videos?name=" + "a".repeat(513), owner));
    }

    @Test
    void createsTheCaseInsensitiveOwnerNameIndex() {
        var definition = jdbc.queryForObject("""
            select indexdef from pg_indexes
            where tablename = 'videos' and indexname = 'videos_owner_name_ci_idx'
            """, String.class);

        assertThat(definition)
            .contains("user_id", "lower((original_filename)::text)", "text_pattern_ops")
            .contains("upload_status", "DELETED");
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

    private static java.util.List<String> names(Map<String, Object> page) {
        var items = (java.util.List<Map<String, Object>>) page.get("items");
        return items.stream().map(item -> item.get("originalFilename").toString()).toList();
    }

    private static String encoded(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void assertValidationError(ResponseEntity<Map> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("VALIDATION_ERROR");
    }

    private static Timestamp ts(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
