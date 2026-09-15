package br.com.fiapx.api.audit;

import org.slf4j.*;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Sanitized audit trail for administrative access. Records administrator,
 * operation, resource type, resource id, UTC timestamp and outcome — never
 * payloads, object keys, signed URLs or personal data.
 */
@Component
public class AdminAuditLogger {
    private static final Logger log = LoggerFactory.getLogger("admin-audit");

    public void record(UUID adminId, String operation, String resourceType, UUID resourceId, String outcome) {
        log.info("adminId={} operation={} resourceType={} resourceId={} occurredAt={} outcome={}",
                adminId, operation, resourceType, resourceId, Instant.now(), outcome);
    }
}
