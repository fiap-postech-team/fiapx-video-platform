package br.com.fiapx.api.job;

import br.com.fiapx.api.audit.AdminAuditLogger;

import java.util.UUID;

import org.springframework.data.domain.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Read-only operational queries for ADMIN. No mutations, no content access:
 * object keys are metadata and never grant download or ownership rights.
 */
@RestController
@RequestMapping("/v1/admin/jobs")
public class AdminJobController {
    private final JobService service;
    private final AdminAuditLogger audit;

    public AdminJobController(JobService s, AdminAuditLogger a) {
        service = s;
        audit = a;
    }

    @GetMapping
    Page<AdminJobResponse> list(@RequestParam(name = "userId", required = false) UUID userId,
                                @RequestParam(name = "status", required = false) Job.Status status,
                                Pageable pageable,
                                @AuthenticationPrincipal Jwt jwt) {
        Page<AdminJobResponse> page = service.search(userId, status, pageable).map(AdminJobResponse::from);
        audit.record(actorId(jwt), "LIST_JOBS", "JOB", null, "SUCCESS");
        return page;
    }

    @GetMapping("/{id}")
    AdminJobResponse get(@PathVariable("id") UUID id, @AuthenticationPrincipal Jwt jwt) {
        try {
            AdminJobResponse response = AdminJobResponse.from(service.getAny(id));
            audit.record(actorId(jwt), "GET_JOB", "JOB", id, "SUCCESS");
            return response;
        } catch (JobNotFoundException ex) {
            audit.record(actorId(jwt), "GET_JOB", "JOB", id, "NOT_FOUND");
            throw ex;
        }
    }

    private static UUID actorId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
