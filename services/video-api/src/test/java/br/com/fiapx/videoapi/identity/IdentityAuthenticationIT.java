package br.com.fiapx.videoapi.identity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers(disabledWithoutDocker = true)
class IdentityAuthenticationIT {
    private static final String PASSWORD = "correct-horse-battery-staple";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @LocalServerPort
    private int port;

    private final TestRestTemplate client = new TestRestTemplate();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void registersLogsInRotatesRefreshAndInvalidatesBearerOnLogout() {
        var email = email();
        var registration = post("/v1/auth/register", credentials(email, PASSWORD));
        assertThat(registration.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(body(registration).get("email")).isEqualTo(email);
        assertThat(body(registration).get("roles")).isEqualTo(List.of("USER"));

        var login = post("/v1/auth/login", credentials(email, PASSWORD));
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        var loginBody = body(login);
        assertThat(loginBody).doesNotContainKeys("password", "refreshToken");
        var firstAccess = (String) loginBody.get("accessToken");
        var refresh = cookie(login, "FIAPX_REFRESH");
        var csrf = cookie(login, "XSRF-TOKEN");
        assertThat(refresh).isNotBlank();
        assertThat(csrf).isNotBlank();
        assertThat(login.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE))
            .anyMatch(header -> header.startsWith("XSRF-TOKEN=") && header.contains("Path=/") && !header.contains("Path=/v1/auth"))
            .anyMatch(header -> header.startsWith("FIAPX_REFRESH=") && header.contains("Path=/v1/auth"));

        var currentUser = currentUser(firstAccess);
        assertThat(currentUser.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body(currentUser)).containsKey("id").containsEntry("email", email).containsEntry("roles", List.of("USER"));
        assertThat(body(currentUser)).doesNotContainKeys("password", "accessToken", "refreshToken");

        var rotated = refresh(refresh, csrf);
        assertThat(rotated.getStatusCode()).isEqualTo(HttpStatus.OK);
        var secondAccess = (String) body(rotated).get("accessToken");
        var rotatedRefresh = cookie(rotated, "FIAPX_REFRESH");
        assertThat(rotatedRefresh).isNotEqualTo(refresh);
        assertThat(currentUser(secondAccess).getStatusCode()).isEqualTo(HttpStatus.OK);

        var logout = authPost("/v1/auth/logout", rotatedRefresh, csrf);
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(cookie(logout, "FIAPX_REFRESH")).isEmpty();
        assertThat(protectedRequest(firstAccess).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(protectedRequest(secondAccess).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(rotatedRefresh, csrf).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(currentUser(secondAccess).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void currentUserRequiresAnActiveBearer() {
        assertThat(currentUser(null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(currentUser("not-a-jwt").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refreshReuseRevokesTheEntireSessionIncludingItsBearer() {
        var login = post("/v1/auth/login", credentials(register(email()), PASSWORD));
        var refresh = cookie(login, "FIAPX_REFRESH");
        var csrf = cookie(login, "XSRF-TOKEN");
        var rotation = refresh(refresh, csrf);
        var rotatedAccess = (String) body(rotation).get("accessToken");

        assertThat(refresh(refresh, csrf).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(protectedRequest(rotatedAccess).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(cookie(rotation, "FIAPX_REFRESH"), csrf).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginFailuresAreGenericAndLockTheAccountAfterFiveAttempts() {
        var unknown = post("/v1/auth/login", credentials(email(), PASSWORD));
        var knownEmail = register(email());
        var wrongPassword = post("/v1/auth/login", credentials(knownEmail, "wrong-password-123"));

        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body(unknown)).isEqualTo(body(wrongPassword));

        for (int attempt = 1; attempt < 5; attempt++) {
            post("/v1/auth/login", credentials(knownEmail, "wrong-password-123"));
        }
        assertThat(post("/v1/auth/login", credentials(knownEmail, PASSWORD)).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String register(String email) {
        assertThat(post("/v1/auth/register", credentials(email, PASSWORD)).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return email;
    }

    private ResponseEntity<String> refresh(String refresh, String csrf) {
        return authPost("/v1/auth/refresh", refresh, csrf);
    }

    private ResponseEntity<String> authPost(String path, String refresh, String csrf) {
        var headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, "XSRF-TOKEN=" + csrf + "; FIAPX_REFRESH=" + refresh);
        headers.set("X-XSRF-TOKEN", csrf);
        return client.exchange(url(path), HttpMethod.POST, new HttpEntity<Void>(headers), String.class);
    }

    private ResponseEntity<String> post(String path, Map<String, String> request) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return client.postForEntity(url(path), new HttpEntity<>(request, headers), String.class);
    }

    private ResponseEntity<String> protectedRequest(String accessToken) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return client.exchange(url("/v1/jobs/" + UUID.randomUUID()), HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private ResponseEntity<String> currentUser(String accessToken) {
        var headers = new HttpHeaders();
        if (accessToken != null) {
            headers.setBearerAuth(accessToken);
        }
        return client.exchange(url("/v1/me"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private String email() {
        return "identity-" + UUID.randomUUID() + "@example.test";
    }

    private Map<String, String> credentials(String email, String password) {
        return Map.of("email", email, "password", password);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> body(ResponseEntity<String> response) {
        try {
            return JSON.readValue(response.getBody(), new TypeReference<>() { });
        } catch (java.io.IOException exception) {
            throw new AssertionError("Expected a JSON response", exception);
        }
    }

    private String cookie(ResponseEntity<String> response, String name) {
        return response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE).stream()
            .filter(value -> value.startsWith(name + "="))
            .map(value -> value.substring(name.length() + 1, value.indexOf(';')))
            .findFirst().orElseThrow();
    }
}
