package br.com.fiapx.videoapi.jobs.adapter.in.http;

import br.com.fiapx.videoapi.jobs.application.CreateJob;
import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.domain.Job;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/jobs")
public class JobController {
    private final CreateJob createJob;
    private final JobStore jobs;
    public JobController(CreateJob createJob, JobStore jobs) { this.createJob = createJob; this.jobs = jobs; }
    @PostMapping @Transactional
    ResponseEntity<JobResponse> create(@Valid @RequestBody CreateJobRequest request, @AuthenticationPrincipal Jwt jwt) {
        var job = createJob.execute(actorId(jwt), request.sourceKey());
        return ResponseEntity.status(201).body(JobResponse.from(job));
    }
    @GetMapping("/{id}")
    JobResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return jobs.findOwned(id, actorId(jwt)).map(JobResponse::from).orElseThrow(JobNotFoundException::new);
    }
    private UUID actorId(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    record CreateJobRequest(@NotBlank String sourceKey) { }
    record JobResponse(UUID id, UUID userId, String sourceKey, String resultKey, String status, java.time.Instant createdAt) {
        static JobResponse from(Job job) { return new JobResponse(job.id(), job.userId(), job.sourceKey(), job.resultKey(), job.status().name(), job.createdAt()); }
    }
}
