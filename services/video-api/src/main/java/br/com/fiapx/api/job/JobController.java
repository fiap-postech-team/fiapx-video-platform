package br.com.fiapx.api.job;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/jobs")
public class JobController {
    private final JobService service;

    public JobController(JobService s) {
        service = s;
    }

    record CreateJob(@NotBlank String sourceKey) {
    }

    @PostMapping
    ResponseEntity<Job> create(@Valid @RequestBody CreateJob body, Authentication auth) {
        Job j = service.create(UUID.fromString(auth.getName()), body.sourceKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(j);
    }

    @GetMapping("/{id}")
    Job get(@PathVariable UUID id) {
        return service.get(id);
    }
}
