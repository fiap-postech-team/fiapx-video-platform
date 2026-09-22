package br.com.fiapx.videoapi.inbox.adapter.in.rabbit;

import br.com.fiapx.videoapi.inbox.domain.JobResultEvent;
import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

final class JobResultMessageParser {
    private final ObjectMapper json;
    private final Clock clock;

    JobResultMessageParser(ObjectMapper json, Clock clock) {
        this.json = json;
        this.clock = clock;
    }

    JobResultEvent parse(String payload) {
        try {
            var root = json.readTree(payload);
            var eventId = UUID.fromString(root.required("eventId").asText());
            var jobId = UUID.fromString(root.required("jobId").asText());
            var status = JobStatus.valueOf(root.required("type").asText());
            var occurredAt = root.hasNonNull("occurredAt")
                ? Instant.parse(root.get("occurredAt").asText()) : clock.instant();
            var resultKey = textOrNull(root, "resultKey");
            var reason = textOrNull(root, "reason");
            if (reason == null) reason = textOrNull(root, "reasonCode");
            return new JobResultEvent(eventId, jobId, status, resultKey, reason, occurredAt, fingerprint(payload));
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid job result event", exception);
        }
    }

    private static String textOrNull(JsonNode root, String field) {
        var value = root.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static String fingerprint(String payload) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }
}
