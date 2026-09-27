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
import org.springframework.amqp.core.Message;

final class JobResultMessageParser {
    private final ObjectMapper json;
    private final Clock clock;

    JobResultMessageParser(ObjectMapper json, Clock clock) {
        this.json = json;
        this.clock = clock;
    }

    JobResultEvent parse(Message message) {
        return parse(new String(message.getBody(), StandardCharsets.UTF_8),
            message.getMessageProperties().getReceivedRoutingKey());
    }

    JobResultEvent parse(String payload) { return parse(payload, null); }

    private JobResultEvent parse(String payload, String routingKey) {
        try {
            var root = json.readTree(payload);
            var eventId = UUID.fromString(root.required("eventId").asText());
            var jobId = UUID.fromString(root.required("jobId").asText());
            var status = JobStatus.valueOf(root.required("type").asText());
            var version = schemaVersion(root);
            if (version != 1) throw new IllegalArgumentException("Unsupported schema version");
            var expected = "video.job." + (status == JobStatus.PROCESSING ? "started" : status.name().toLowerCase()) + ".v1";
            if (routingKey != null && !expected.equals(routingKey)) throw new IllegalArgumentException("Routing key does not match event type");
            var occurredAt = root.hasNonNull("occurredAt")
                ? Instant.parse(root.get("occurredAt").asText()) : clock.instant();
            var resultKey = textOrNull(root, "resultKey");
            if (status == JobStatus.COMPLETED && (resultKey == null || resultKey.isBlank())) throw new IllegalArgumentException("Missing resultKey");
            if (status != JobStatus.COMPLETED && resultKey != null) throw new IllegalArgumentException("Unexpected resultKey");
            var reason = textOrNull(root, "reason");
            if (reason == null) reason = textOrNull(root, "reasonCode");
            var correlation = root.hasNonNull("correlationId") ? UUID.fromString(root.get("correlationId").asText()) : jobId;
            return new JobResultEvent(eventId, jobId, status, resultKey, sanitize(reason), occurredAt, fingerprint(payload),
                routingKey == null ? expected : routingKey, version, correlation);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid job result event", exception);
        }
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) return null;
        var code = value.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (code) {
            case "INVALID_MEDIA", "SOURCE_UNAVAILABLE", "RESULT_UPLOAD_FAILED", "PROCESSING_FAILED", "PROCESSING_ERROR" -> code;
            default -> "PROCESSING_FAILED";
        };
    }

    /** Accepts AsyncAPI integer {@code 1} and the processor wire value {@code "1.0"}. */
    private static int schemaVersion(JsonNode root) {
        if (!root.hasNonNull("schemaVersion")) return 1;
        var node = root.get("schemaVersion");
        if (node.isNumber()) return node.intValue();
        var text = node.asText();
        if ("1".equals(text) || "1.0".equals(text)) return 1;
        return -1;
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
