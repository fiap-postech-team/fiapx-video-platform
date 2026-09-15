package br.com.fiapx.videoapi.foundation.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(
    value = ApiExceptionHandlerMvcTest.FixtureController.class,
    properties = {
        "spring.datasource.url=jdbc:postgresql://database.invalid:5432/video",
        "spring.datasource.username=application",
        "spring.datasource.password=test-only"
    }
)
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(OutputCaptureExtension.class)
@Import({
    ApiExceptionHandlerMvcTest.FixtureController.class,
    ApiExceptionHandler.class,
    ApiProblemFactory.class
})
class ApiExceptionHandlerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsProblemDetailForValidationFailure() throws Exception {
        mockMvc.perform(post("/fixture/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sourceKey\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void serializesTheBoundaryResponseWithoutInternalFields() throws Exception {
        mockMvc.perform(post("/fixture/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sourceKey\":\"videos/source.mp4\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourceKey").value("videos/source.mp4"))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    void sanitizesUnexpectedFailure(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/fixture/failure"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.detail").value("Não foi possível concluir a solicitação."))
            .andExpect(content().string(not(containsString("internal database failure"))));

        assertThat(output).contains("method=GET path=/fixture/failure")
            .doesNotContain("internal database failure");
    }

    @RestController
    public static class FixtureController {

        @PostMapping("/fixture/validation")
        FixtureResponse validate(@Valid @RequestBody FixtureRequest request) {
            return new FixtureResponse(request.sourceKey());
        }

        @GetMapping("/fixture/failure")
        void fail() {
            throw new IllegalStateException("internal database failure");
        }
    }

    record FixtureRequest(@NotBlank String sourceKey) {}

    record FixtureResponse(String sourceKey) {}
}
