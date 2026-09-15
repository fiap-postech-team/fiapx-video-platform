package br.com.fiapx.videoapi.foundation.http;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts failures reaching the MVC boundary into the shared sanitized error contract.
 */
@RestControllerAdvice
public final class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private final ApiProblemFactory problems;

    /**
     * Creates the handler with the shared problem factory.
     *
     * @param problems shared problem factory
     */
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
        LOGGER.error(
            "Unexpected HTTP failure method={} path={} exceptionType={}",
            request.getMethod(),
            request.getRequestURI(),
            exception.getClass().getName()
        );
        return response(ProblemType.INTERNAL_ERROR, request);
    }

    private ResponseEntity<ProblemDetail> response(ProblemType type, HttpServletRequest request) {
        return ResponseEntity.status(type.status())
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problems.create(type, URI.create(request.getRequestURI())));
    }
}
