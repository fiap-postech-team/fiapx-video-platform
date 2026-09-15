package br.com.fiapx.videoapi.foundation.openapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
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
class LocalSwaggerIT {

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
    void exposesStaticOpenApiAndSwaggerOnlyInLocalProfile() {
        var specification = get("/openapi.yaml", String.class);
        var swagger = get("/swagger-ui.html", String.class);
        var swaggerConfiguration = get("/v3/api-docs/swagger-config", String.class);
        var dynamicDocs = get("/v3/api-docs", String.class);

        assertThat(specification.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(specification.getBody()).contains("openapi: 3.1.0");
        assertThat(swagger.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(swagger.getBody()).contains("swagger-initializer.js");
        assertThat(swaggerConfiguration.getBody()).contains("/openapi.yaml", "\"tryItOutEnabled\":false");
        assertThat(dynamicDocs.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private <T> org.springframework.http.ResponseEntity<T> get(String path, Class<T> bodyType) {
        return client.getForEntity("http://localhost:" + port + path, bodyType);
    }
}
