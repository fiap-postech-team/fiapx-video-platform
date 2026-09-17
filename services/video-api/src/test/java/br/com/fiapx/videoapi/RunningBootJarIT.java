package br.com.fiapx.videoapi;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static br.com.fiapx.videoapi.BootJarIT.packagedJar;

@Testcontainers(disabledWithoutDocker = true)
class RunningBootJarIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void runsPackagedJarWithHealthAndLocalDocumentation() throws Exception {
        var port = availableBackendPort();
        var process = startJar(port);

        try {
            var health = awaitOk(port, "/actuator/health");
            var openApi = awaitOk(port, "/openapi.yaml");
            var swagger = awaitOk(port, "/swagger-ui.html");

            assertThat(health.body()).contains("\"status\":\"UP\"");
            assertThat(openApi.body()).contains("openapi: 3.1.0");
            assertThat(swagger.body()).contains("swagger-initializer.js");
        } finally {
            stop(process);
        }
        assertThat(availableBackendPort()).isEqualTo(port);
    }

    private Process startJar(int port) throws Exception {
        return new ProcessBuilder(
            PathSupport.javaExecutable(), "-jar", packagedJar().toString(),
            "--spring.profiles.active=local", "--server.port=" + port,
            "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
            "--spring.datasource.username=" + POSTGRES.getUsername(),
            "--spring.datasource.password=" + POSTGRES.getPassword()
        ).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
    }

    private HttpResponse<String> awaitOk(int port, String path) throws Exception {
        var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build();
        var deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline) {
            try {
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return response;
                }
            } catch (java.io.IOException ignored) {
                LockSupport.parkNanos(Duration.ofMillis(100).toNanos());
            }
        }
        throw new IllegalStateException("Application did not expose " + path + " before timeout");
    }

    private int availableBackendPort() throws java.io.IOException {
        for (var port = 3000; port <= 3099; port++) {
            try (var socket = new ServerSocket(port)) {
                return socket.getLocalPort();
            } catch (java.net.BindException ignored) {
                // Continue within the worktree-specific backend range.
            }
        }
        throw new IllegalStateException("No backend port available in range 3000-3099");
    }

    private void stop(Process process) throws InterruptedException {
        process.destroy();
        if (!process.waitFor(20, TimeUnit.SECONDS)) {
            process.destroyForcibly();
        }
    }

    private static final class PathSupport {
        private static String javaExecutable() {
            return java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java").toString();
        }
    }
}
