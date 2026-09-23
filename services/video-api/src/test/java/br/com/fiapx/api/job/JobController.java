package br.com.fiapx.api.job;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class JobController {
    private final JobService service;

    public JobController(JobService service) {
        this.service = service;
    }

    @PostMapping("/jobs")
    ResponseEntity<JobResponse> create(@RequestBody CreateJob request, @AuthenticationPrincipal Jwt jwt) {
        Job job = service.create(actorId(jwt), request.sourceKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(JobResponse.from(job));
    }

    @GetMapping("/jobs")
    List<JobResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.listOwned(actorId(jwt)).stream().map(JobResponse::from).toList();
    }

    @GetMapping("/jobs/{id}")
    JobResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return JobResponse.from(service.getOwned(id, actorId(jwt)));
    }

    private static UUID actorId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    public record CreateJob(String sourceKey) {
    }
}
