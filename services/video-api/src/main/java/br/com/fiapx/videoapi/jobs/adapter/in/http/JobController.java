package br.com.fiapx.videoapi.jobs.adapter.in.http;

import br.com.fiapx.videoapi.jobs.application.CreateJob;
import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.domain.Job;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import br.com.fiapx.videoapi.identity.domain.AuthenticatedIdentity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/jobs")
public class JobController {
    private final CreateJob createJob;
    private final JobStore jobs;
    private final JobTransactionExecutor transactions;

    public JobController(CreateJob createJob, JobStore jobs, JobTransactionExecutor transactions) {
        this.createJob = createJob;
        this.jobs = jobs;
        this.transactions = transactions;
    }

    JobController(CreateJob createJob, JobStore jobs) {
        this(createJob, jobs, Supplier::get);
    }
    @PostMapping
    ResponseEntity<JobResponse> create(@Valid @RequestBody CreateJobRequest request,
                                       @RequestHeader(value = "Idempotency-Key", required = false) String key,
                                       @AuthenticationPrincipal AuthenticatedIdentity identity) {
        var job = transactions.execute(() -> createJob.execute(identity.userId(), request.sourceKey(), validKey(key)));
        return ResponseEntity.status(201).body(JobResponse.from(job));
    }
    @GetMapping("/{id}")
    JobResponse get(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedIdentity identity) {
        return jobs.findOwned(id, identity.userId()).map(JobResponse::from).orElseThrow(JobNotFoundException::new);
    }
    @GetMapping
    JobPage list(@RequestParam(required = false) String cursor,
                 @RequestParam(defaultValue = "20") int limit,
                 @AuthenticationPrincipal AuthenticatedIdentity identity) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Invalid page size");
        }
        var after = cursor == null ? null : JobCursor.decode(cursor);
        var jobsPage = jobs.findOwnedPage(identity.userId(), after == null ? null : after.createdAt(),
                after == null ? null : after.id(), limit);
        return JobPage.from(jobsPage, limit);
    }
    record CreateJobRequest(@NotBlank String sourceKey) { }
    record JobResponse(UUID id, UUID userId, String sourceKey, String resultKey, String status, java.time.Instant createdAt) {
        static JobResponse from(Job job) { return new JobResponse(job.id(), job.userId(), job.sourceKey(), job.resultKey(), job.status().name(), job.createdAt()); }
    }
    record JobPage(List<JobResponse> items, String nextCursor) {
        static JobPage from(List<Job> jobs, int limit) {
            var items = jobs.stream().map(JobResponse::from).toList();
            var next = jobs.size() < limit ? null : cursorFor(jobs.getLast());
            return new JobPage(items, next);
        }

        private static String cursorFor(Job job) {
            return new JobCursor(job.createdAt(), job.id()).encode();
        }
    }

    private String validKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        if (key.length() > 128) {
            throw new IllegalArgumentException("Invalid idempotency key");
        }
        return key;
    }
}
