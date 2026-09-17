package br.com.fiapx.videoapi.foundation.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    value = FoundationSecurityConfigurationMvcTest.HealthFixtureController.class,
    properties = {
        "spring.datasource.url=jdbc:postgresql://database.invalid:5432/video",
        "spring.datasource.username=application",
        "spring.datasource.password=test-only"
    }
)
@Import({
    FoundationSecurityConfigurationMvcTest.HealthFixtureController.class,
    FoundationSecurityConfiguration.class,
    ProblemDetailAccessDeniedHandler.class,
    ProblemDetailAuthenticationEntryPoint.class,
    br.com.fiapx.videoapi.foundation.http.ApiProblemFactory.class
})
class FoundationSecurityConfigurationMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void permitsHealthWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void returnsUnauthorizedProblemForAnonymousDeniedRequest() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser
    void permitsAuthenticatedBusinessRequests() throws Exception {
        mockMvc.perform(get("/v1/jobs"))
            .andExpect(status().isOk());
    }

    @RestController
    public static class HealthFixtureController {

        @GetMapping("/actuator/health")
        HealthStatus health() {
            return new HealthStatus("UP");
        }

        @GetMapping("/v1/jobs")
        HealthStatus jobs() {
            return new HealthStatus("UP");
        }
    }

    record HealthStatus(String status) {
    }
}
