package br.com.fiapx.api.error;

import br.com.fiapx.api.job.JobNotFoundException;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Maps domain exceptions to RFC 7807 ProblemDetail responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JobNotFoundException.class)
    ProblemDetail notFound(JobNotFoundException ex) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Recurso não encontrado.");
        body.setTitle("Não encontrado");
        return body;
    }

    @ExceptionHandler(StateConflictException.class)
    ProblemDetail conflict(StateConflictException ex) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        body.setTitle("Conflito de estado");
        return body;
    }
}
