package br.com.fiapx.api.job;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.security.jwt-secret=" + JobAuthorizationIntegrationTest.SECRET,
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.outbox.enabled=false"})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class JobAuthorizationIntegrationTest {
    static final String SECRET = "test-secret-key-with-at-least-32-bytes!";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JobRepository jobs;

    @BeforeEach
    void cleanDatabase() {
        jobs.deleteAll();
    }

    private static String token(String secret, UUID subject, Object roles) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(subject.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
        if (roles != null) {
            claims.claim("roles", roles);
        }
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secret.getBytes(StandardCharsets.UTF_8)));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
    }

    private static String userToken(UUID subject) {
        return token(SECRET, subject, List.of("USER"));
    }

    private static String adminToken(UUID subject) {
        return token(SECRET, subject, List.of("ADMIN"));
    }

    private ResultActions authenticatedGet(String url, String token) throws Exception {
        return mvc.perform(get(url).header("Authorization", "Bearer " + token));
    }

    @Test
    void ownerReadsOwnJob() throws Exception {
        UUID owner = UUID.randomUUID();
        Job job = jobs.save(new Job(owner, "videos/owned.mp4"));

        authenticatedGet("/v1/jobs/" + job.getId(), userToken(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(job.getId().toString()))
                .andExpect(jsonPath("$.userId").value(owner.toString()));
    }

    @Test
    void jobOfAnotherOwnerIsIndistinguishableFromMissing() throws Exception {
        Job job = jobs.save(new Job(UUID.randomUUID(), "videos/secret.mp4"));
        UUID stranger = UUID.randomUUID();

        // Same status, title and detail for foreign and missing ids: no existence oracle.
        authenticatedGet("/v1/jobs/" + job.getId(), userToken(stranger))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Não encontrado"))
                .andExpect(jsonPath("$.detail").value("Recurso não encontrado."));
        authenticatedGet("/v1/jobs/" + UUID.randomUUID(), userToken(stranger))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Não encontrado"))
                .andExpect(jsonPath("$.detail").value("Recurso não encontrado."));
    }

    @Test
    void listReturnsOnlyOwnJobs() throws Exception {
        UUID owner = UUID.randomUUID();
        jobs.save(new Job(owner, "videos/mine.mp4"));
        jobs.save(new Job(UUID.randomUUID(), "videos/theirs.mp4"));

        authenticatedGet("/v1/jobs", userToken(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(owner.toString()));
    }

    @Test
    void createAssignsJwtSubjectAsOwner() throws Exception {
        UUID owner = UUID.randomUUID();

        mvc.perform(post("/v1/jobs")
                        .header("Authorization", "Bearer " + userToken(owner))
                        .contentType("application/json")
                        .content("{\"sourceKey\":\"videos/new.mp4\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(owner.toString()));
    }

    @Test
    void userRoleIsForbiddenOnAdminRoutes() throws Exception {
        authenticatedGet("/v1/admin/jobs", userToken(UUID.randomUUID()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void adminListsAndFiltersJobs() throws Exception {
        UUID ownerA = UUID.randomUUID();
        UUID ownerB = UUID.randomUUID();
        Job pending = jobs.save(new Job(ownerA, "videos/a.mp4"));
        Job completed = new Job(ownerB, "videos/b.mp4");
        completed.apply(Job.Status.COMPLETED, "results/b.zip");
        jobs.save(completed);
        UUID admin = UUID.randomUUID();

        authenticatedGet("/v1/admin/jobs", adminToken(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        authenticatedGet("/v1/admin/jobs?userId=" + ownerA, adminToken(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(pending.getId().toString()));

        authenticatedGet("/v1/admin/jobs?status=COMPLETED", adminToken(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));
    }

    @Test
    void adminReadsJobOfAnyOwner() throws Exception {
        Job job = jobs.save(new Job(UUID.randomUUID(), "videos/any.mp4"));

        authenticatedGet("/v1/admin/jobs/" + job.getId(), adminToken(UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(job.getId().toString()));
    }

    @Test
    void adminHasNoMutationEndpoint() throws Exception {
        mvc.perform(post("/v1/admin/jobs")
                        .header("Authorization", "Bearer " + adminToken(UUID.randomUUID()))
                        .contentType("application/json")
                        .content("{\"sourceKey\":\"videos/x.mp4\"}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void missingTokenReturns401() throws Exception {
        mvc.perform(get("/v1/jobs/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void malformedTokenReturns401() throws Exception {
        authenticatedGet("/v1/jobs/" + UUID.randomUUID(), "not-a-jwt")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithWrongKeyReturns401() throws Exception {
        String forged = token("another-secret-key-with-32-bytes!!", UUID.randomUUID(), List.of("ADMIN"));

        authenticatedGet("/v1/admin/jobs", forged).andExpect(status().isUnauthorized());
    }

    @Test
    void rolesClaimWithUnexpectedTypeGrantsNoRole() throws Exception {
        String tampered = token(SECRET, UUID.randomUUID(), "ADMIN");

        authenticatedGet("/v1/admin/jobs", tampered).andExpect(status().isForbidden());
    }

    @Test
    void unknownRoleValueGrantsNoAccess() throws Exception {
        String tampered = token(SECRET, UUID.randomUUID(), List.of("SUPERUSER"));

        authenticatedGet("/v1/admin/jobs", tampered).andExpect(status().isForbidden());
    }

    @Test
    void adminAccessIsAuditedWithoutSensitiveData() throws Exception {
        ch.qos.logback.classic.Logger auditLogger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("admin-audit");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        auditLogger.addAppender(appender);
        try {
            UUID admin = UUID.randomUUID();
            Job job = jobs.save(new Job(UUID.randomUUID(), "videos/audited-secret-key.mp4"));

            authenticatedGet("/v1/admin/jobs/" + job.getId(), adminToken(admin)).andExpect(status().isOk());
            authenticatedGet("/v1/admin/jobs/" + UUID.randomUUID(), adminToken(admin)).andExpect(status().isNotFound());

            assertThat(appender.list).hasSize(2);
            ILoggingEvent success = appender.list.get(0);
            assertThat(success.getFormattedMessage())
                    .contains("adminId=" + admin)
                    .contains("operation=GET_JOB")
                    .contains("resourceType=JOB")
                    .contains("resourceId=" + job.getId())
                    .contains("outcome=SUCCESS");
            assertThat(appender.list.get(1).getFormattedMessage()).contains("outcome=NOT_FOUND");
            assertThat(appender.list)
                    .allSatisfy(event -> assertThat(event.getFormattedMessage()).doesNotContain("audited-secret-key"));
        } finally {
            auditLogger.detachAppender(appender);
        }
    }
}
