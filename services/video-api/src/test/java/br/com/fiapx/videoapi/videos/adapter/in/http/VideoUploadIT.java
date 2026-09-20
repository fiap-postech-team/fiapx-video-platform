package br.com.fiapx.videoapi.videos.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers(disabledWithoutDocker = true)
class VideoUploadIT extends VideoUploadIntegrationSupport {
    @Test
    void uploadsDirectlyConfirmsIdempotentlyAndCreatesOwnedJob() throws Exception {
        var owner = token();
        var other = token();
        var bytes = "small video fixture".getBytes(StandardCharsets.UTF_8);
        var created = post("/v1/videos/uploads", request(bytes.length), owner);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var upload = created.getBody();
        assertThat(upload.get("sourceKey")).asString().startsWith("users/").endsWith("/source");
        assertThat(upload.get("method")).isEqualTo("PUT");
        assertThat(post(confirmPath(upload), null, other).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(put(upload, bytes)).isEqualTo(200);
        assertThat(put(upload, "different".getBytes(StandardCharsets.UTF_8))).isEqualTo(412);

        var confirmed = post(confirmPath(upload), null, owner);
        assertThat(confirmed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(confirmed.getBody().get("status")).isEqualTo("UPLOADED");
        assertThat(post(confirmPath(upload), null, owner).getBody()).isEqualTo(confirmed.getBody());
        assertThat(post("/v1/jobs", Map.of("sourceKey", upload.get("sourceKey")), owner)
            .getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(post("/v1/jobs", Map.of("sourceKey", upload.get("sourceKey")), other)
            .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsMissingAndMismatchedObjects() throws Exception {
        var owner = token();
        var upload = post("/v1/videos/uploads", request(30), owner).getBody();

        assertThat(post(confirmPath(upload), null, owner).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(put(upload, "short".getBytes(StandardCharsets.UTF_8))).isEqualTo(200);
        assertThat(post(confirmPath(upload), null, owner).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(post("/v1/jobs", Map.of("sourceKey", upload.get("sourceKey")), owner)
            .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void requiresAuthenticationForUpload() {
        assertThat(client.postForEntity(url("/v1/videos/uploads"), request(10), Map.class)
            .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void acceptsSignedSha256AndConfirmsObservedChecksum() throws Exception {
        var owner = token();
        var bytes = "checksum fixture".getBytes(StandardCharsets.UTF_8);
        var request = new HashMap<>(request(bytes.length));
        request.put("checksumSha256", HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(bytes)));
        var upload = post("/v1/videos/uploads", request, owner).getBody();

        assertThat(upload.get("headers").toString()).containsIgnoringCase("x-amz-checksum-sha256");
        assertThat(put(upload, bytes)).isEqualTo(200);
        assertThat(post(confirmPath(upload), null, owner).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void rejectsInvalidDeclaredMedia() {
        var owner = token();
        assertThat(post("/v1/videos/uploads", request(500000001), owner).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(post("/v1/videos/uploads", Map.of("originalFilename", "clip.mov",
            "contentType", "application/octet-stream", "sizeBytes", 10), owner).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private Map<String, Object> request(int size) {
        return Map.of("originalFilename", "clip.mp4", "contentType", "video/mp4", "sizeBytes", size);
    }

    private String confirmPath(Map<String, Object> upload) {
        return "/v1/videos/" + upload.get("videoId") + "/confirm";
    }
}
