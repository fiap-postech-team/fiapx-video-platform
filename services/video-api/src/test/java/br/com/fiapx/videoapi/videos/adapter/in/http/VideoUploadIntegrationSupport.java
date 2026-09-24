package br.com.fiapx.videoapi.videos.adapter.in.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class VideoUploadIntegrationSupport {
    static final String USER = "uploadtest";
    static final String PASSWORD = "uploadtest-secret";
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");
    @Container
    static final GenericContainer<?> MINIO = new GenericContainer<>("pgsty/minio:RELEASE.2026-08-04T00-00-00Z")
        .withEnv("MINIO_ROOT_USER", USER).withEnv("MINIO_ROOT_PASSWORD", PASSWORD)
        .withCommand("server", "/data").withExposedPorts(9000);

    @LocalServerPort
    int port;
    @Autowired
    software.amazon.awssdk.services.s3.S3Client storage;
    final TestRestTemplate client = new TestRestTemplate();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.video.internal-endpoint", VideoUploadIntegrationSupport::minioUrl);
        registry.add("app.video.public-endpoint", VideoUploadIntegrationSupport::minioUrl);
        registry.add("app.video.access-key", () -> USER);
        registry.add("app.video.secret-key", () -> PASSWORD);
        registry.add("app.video.storage-mode", () -> "s3");
    }

    static String minioUrl() {
        return "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
    }

    @BeforeEach
    void bucket() {
        try {
            storage.createBucket(request -> request.bucket("videos"));
        } catch (software.amazon.awssdk.services.s3.model.S3Exception exception) {
            if (exception.statusCode() != 409) {
                throw exception;
            }
        }
    }

    String token() {
        var credentials = Map.of("email", "upload-" + UUID.randomUUID() + "@example.test",
            "password", "correct-horse-battery-staple");
        client.postForEntity(url("/v1/auth/register"), credentials, Map.class);
        return (String) client.postForEntity(url("/v1/auth/login"), credentials, Map.class)
            .getBody().get("accessToken");
    }

    ResponseEntity<Map> post(String path, Object body, String token) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return client.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    }

    ResponseEntity<Map> get(String path, String token) {
        var headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return client.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }

    int put(Map<String, Object> upload, byte[] bytes) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create((String) upload.get("uploadUrl")))
            .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes));
        ((Map<String, String>) upload.get("headers")).forEach(builder::header);
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    String url(String path) {
        return "http://localhost:" + port + path;
    }
}
