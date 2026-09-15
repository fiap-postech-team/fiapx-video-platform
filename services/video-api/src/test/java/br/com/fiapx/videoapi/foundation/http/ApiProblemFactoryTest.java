package br.com.fiapx.videoapi.foundation.http;

import java.net.URI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiProblemFactoryTest {

    private final ApiProblemFactory factory = new ApiProblemFactory();

    @Test
    void createsAStableSanitizedValidationProblem() {
        var problem = factory.create(ProblemType.VALIDATION_ERROR, URI.create("/v1/jobs"));

        assertThat(problem.getType()).isEqualTo(URI.create("urn:fiapx:problem:validation"));
        assertThat(problem.getTitle()).isEqualTo("Requisição inválida");
        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("Um ou mais dados informados são inválidos.");
        assertThat(problem.getInstance()).isEqualTo(URI.create("/v1/jobs"));
        assertThat(problem.getProperties()).containsEntry("code", "VALIDATION_ERROR");
    }
}
