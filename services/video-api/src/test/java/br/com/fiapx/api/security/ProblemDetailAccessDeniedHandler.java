package br.com.fiapx.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;

import java.io.IOException;

import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Renders 403 responses as RFC 7807 ProblemDetail. */
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper mapper;

    public ProblemDetailAccessDeniedHandler(ObjectMapper m) {
        mapper = m;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Identidade autenticada sem o papel exigido para esta rota.");
        body.setTitle("Acesso negado");
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), body);
    }
}
