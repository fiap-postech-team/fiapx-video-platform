package br.com.fiapx.videoapi.foundation.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import br.com.fiapx.videoapi.foundation.http.ApiProblemFactory;
import br.com.fiapx.videoapi.foundation.http.ProblemType;

@Component
public final class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ApiProblemFactory problems;
    private final ObjectMapper objectMapper;

    public ProblemDetailAuthenticationEntryPoint(
        ApiProblemFactory problems,
        ObjectMapper objectMapper
    ) {
        this.problems = problems;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException {
        write(response, problems.create(ProblemType.UNAUTHORIZED, URI.create(request.getRequestURI())));
    }

    private void write(HttpServletResponse response, Object body) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
