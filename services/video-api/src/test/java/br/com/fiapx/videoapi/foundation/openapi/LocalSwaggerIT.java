package br.com.fiapx.videoapi.foundation.openapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LocalSwaggerIT.TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
@AutoConfigureMockMvc
class LocalSwaggerIT {
    @Autowired
    private MockMvc mvc;

    @Test
    void exposesStaticOpenApiAndSwaggerOnlyInLocalProfile() throws Exception {
        mvc.perform(get("/openapi.yaml"))
            .andExpect(status().isOk());
        mvc.perform(get("/openapi.yaml"))
            .andExpect(content().string(containsString("openapi: 3.1.0")));
        mvc.perform(get("/swagger-ui.html"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/swagger-ui/index.html"));
        mvc.perform(get("/v3/api-docs/swagger-config"))
            .andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isUnauthorized());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        RabbitAutoConfiguration.class
    })
    @Import({
        br.com.fiapx.videoapi.foundation.http.ApiProblemFactory.class,
        br.com.fiapx.videoapi.foundation.security.FoundationSecurityConfiguration.class,
        br.com.fiapx.videoapi.foundation.security.ProblemDetailAccessDeniedHandler.class,
        br.com.fiapx.videoapi.foundation.security.ProblemDetailAuthenticationEntryPoint.class
    })
    static class TestApplication {
    }
}
