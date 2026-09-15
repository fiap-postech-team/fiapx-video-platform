package br.com.fiapx.videoapi.foundation.http;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class ApiExceptionHandler {

    private final ApiProblemFactory problems;

    public ApiExceptionHandler(ApiProblemFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        return response(ProblemType.VALIDATION_ERROR, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(
        Exception exception,
        HttpServletRequest request
    ) {
        return response(ProblemType.INTERNAL_ERROR, request);
    }

    private ResponseEntity<ProblemDetail> response(ProblemType type, HttpServletRequest request) {
        return ResponseEntity.status(type.status())
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problems.create(type, URI.create(request.getRequestURI())));
    }
}
