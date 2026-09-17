package br.com.fiapx.videoapi.outbox.adapter.out.persistence;

import br.com.fiapx.videoapi.outbox.application.port.out.OutboxStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import br.com.fiapx.videoapi.outbox.domain.OutboxClaim;

@Component
public final class JpaOutboxStore implements OutboxStore {
    private final SpringDataOutboxRepository events;
    private final ObjectMapper json;
    public JpaOutboxStore(SpringDataOutboxRepository events, ObjectMapper json) { this.events = events; this.json = json; }
    public void append(UUID eventId, UUID jobId, UUID userId, String sourceKey) {
        append(eventId, jobId, userId, null, sourceKey);
    }

    public void append(UUID eventId, UUID jobId, UUID userId, UUID videoId, String sourceKey) {
        try { events.save(new OutboxEventEntity(eventId, jobId, json.writeValueAsString(payload(eventId, jobId, userId, videoId, sourceKey)), Instant.now())); }
        catch (Exception exception) { throw new IllegalStateException("Cannot serialize job event", exception); }
    }

    private Map<String, Object> payload(UUID eventId, UUID jobId, UUID userId, UUID videoId, String sourceKey) {
        if (videoId == null) return Map.of("eventId", eventId, "jobId", jobId, "userId", userId, "sourceKey", sourceKey);
        return Map.of("eventId", eventId, "jobId", jobId, "userId", userId, "videoId", videoId, "sourceKey", sourceKey);
    }

    @Transactional
    public List<OutboxClaim> claimReady(int limit, String instanceId, Instant now, Instant expiresAt) {
        return events.lockReady(now, limit).stream().map(event -> claim(event, instanceId, now, expiresAt)).toList();
    }

    @Transactional
    public boolean markPublished(UUID eventId, UUID claimToken, Instant publishedAt) {
        return events.findById(eventId).filter(event -> event.isClaimedBy(claimToken)).map(event -> publish(event, publishedAt)).orElse(false);
    }

    @Transactional
    public boolean scheduleRetry(UUID eventId, UUID claimToken, Instant nextAttemptAt, String errorCode) {
        return events.findById(eventId).filter(event -> event.isClaimedBy(claimToken)).map(event -> retry(event, nextAttemptAt, errorCode)).orElse(false);
    }

    private OutboxClaim claim(OutboxEventEntity event, String instance, Instant now, Instant expiresAt) {
        var view = event.claim(UUID.randomUUID(), instance, now, expiresAt);
        return new OutboxClaim(view.id(), view.token(), view.routingKey(), view.payload(), view.occurredAt());
    }

    private boolean publish(OutboxEventEntity event, Instant at) { event.publish(at); return true; }
    private boolean retry(OutboxEventEntity event, Instant at, String code) { event.retry(at, code); return true; }
}
