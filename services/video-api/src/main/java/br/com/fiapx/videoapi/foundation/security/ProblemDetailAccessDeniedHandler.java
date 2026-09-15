package br.com.fiapx.videoapi.foundation.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import br.com.fiapx.videoapi.foundation.http.ApiProblemFactory;
import br.com.fiapx.videoapi.foundation.http.ProblemType;

/**
 * Writes the shared problem contract when an authenticated request is forbidden.
 */
@Component
public final class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private final ApiProblemFactory problems;
    private final ObjectMapper objectMapper;

    /**
     * Creates the handler with the shared response collaborators.
     *
     * @param problems shared problem factory
     * @param objectMapper configured JSON mapper
     */
    public ProblemDetailAccessDeniedHandler(
        ApiProblemFactory problems,
        ObjectMapper objectMapper
    ) {
        this.problems = problems;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException exception
    ) throws IOException {
        write(response, problems.create(ProblemType.ACCESS_DENIED, URI.create(request.getRequestURI())));
    }

    private void write(HttpServletResponse response, ProblemDetail body) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
