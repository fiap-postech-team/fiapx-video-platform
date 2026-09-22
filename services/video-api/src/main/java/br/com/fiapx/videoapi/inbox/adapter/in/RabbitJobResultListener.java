package br.com.fiapx.videoapi.inbox.adapter.in;

import br.com.fiapx.videoapi.inbox.application.ProcessJobResult;
import br.com.fiapx.videoapi.inbox.domain.JobResultEvent;
import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public final class RabbitJobResultListener {
    private final ObjectMapper json;
    private final ProcessJobResult processor;
    public RabbitJobResultListener(ObjectMapper json, ProcessJobResult processor) { this.json = json; this.processor = processor; }

    @RabbitListener(queues = "video.api.results.v1")
    @Transactional
    public void receive(String body) throws Exception {
        var node = json.readTree(body);
        var type = JobStatus.valueOf(node.required("type").asText());
        var occurred = node.has("occurredAt") ? Instant.parse(node.get("occurredAt").asText()) : Instant.now();
        var event = new JobResultEvent(UUID.fromString(node.required("eventId").asText()),
            UUID.fromString(node.required("jobId").asText()), type, text(node, "resultKey"),
            text(node, "reason"), occurred, fingerprint(body));
        processor.execute(event);
    }

    private String text(com.fasterxml.jackson.databind.JsonNode node, String name) {
        return node.hasNonNull(name) ? node.get(name).asText() : null;
    }
    private String fingerprint(String body) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(body.getBytes(StandardCharsets.UTF_8)));
    }
}
