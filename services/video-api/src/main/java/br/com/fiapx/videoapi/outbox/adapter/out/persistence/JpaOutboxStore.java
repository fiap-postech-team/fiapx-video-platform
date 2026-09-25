package br.com.fiapx.videoapi.outbox.adapter.out.persistence;

import br.com.fiapx.videoapi.outbox.application.port.out.OutboxStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import br.com.fiapx.videoapi.outbox.domain.OutboxClaim;

@Component
public class JpaOutboxStore implements OutboxStore {
    private final SpringDataOutboxRepository events;
    private final ObjectMapper json;
    private final Clock clock;
    public JpaOutboxStore(SpringDataOutboxRepository events, ObjectMapper json) {
        this(events, json, Clock.systemUTC());
    }
    @Autowired
    public JpaOutboxStore(SpringDataOutboxRepository events, ObjectMapper json, Clock clock) {
        this.events = events; this.json = json; this.clock = clock;
    }
    public void append(UUID eventId, UUID jobId, UUID userId, String sourceKey) {
        append(eventId, jobId, userId, null, sourceKey, clock.instant());
    }

    public void append(UUID eventId, UUID jobId, UUID userId, UUID videoId, String sourceKey) {
        append(eventId, jobId, userId, videoId, sourceKey, clock.instant());
    }

    public void append(UUID eventId, UUID jobId, UUID userId, UUID videoId, String sourceKey,
                       Instant occurredAt) {
        append(eventId, jobId, userId, videoId, sourceKey, occurredAt, jobId);
    }

    public void append(UUID eventId, UUID jobId, UUID userId, UUID videoId, String sourceKey,
                       Instant occurredAt, UUID correlationId) {
        var effectiveCorrelationId = correlationId == null ? jobId : correlationId;
        var payload = payload(eventId, jobId, userId, videoId, sourceKey, occurredAt, effectiveCorrelationId);
        try { events.save(new OutboxEventEntity(eventId, jobId, json.writeValueAsString(payload), payload, occurredAt, effectiveCorrelationId)); }
        catch (Exception exception) { throw new IllegalStateException("Cannot serialize job event", exception); }
    }

    private Map<String, Object> payload(UUID eventId, UUID jobId, UUID userId, UUID videoId,
                                        String sourceKey, Instant occurredAt, UUID correlationId) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("eventId", eventId);
        payload.put("jobId", jobId);
        payload.put("userId", userId);
        if (videoId != null) payload.put("videoId", videoId);
        payload.put("sourceKey", sourceKey);
        payload.put("type", "video.job.requested.v1");
        payload.put("schemaVersion", 1);
        payload.put("occurredAt", occurredAt.toString());
        payload.put("correlationId", correlationId);
        return payload;
    }

    @Transactional
    public List<OutboxClaim> claimReady(int limit, String instanceId, Instant now, Instant expiresAt) {
        return events.lockReady(now, limit).stream().map(event -> claim(event, instanceId, now, expiresAt)).toList();
    }

    @Transactional
    public boolean markPublished(UUID eventId, UUID claimToken, Instant publishedAt) {
        return events.markPublished(eventId, claimToken, publishedAt) == 1;
    }

    @Transactional
    public boolean scheduleRetry(UUID eventId, UUID claimToken, Instant nextAttemptAt, String errorCode) {
        return events.scheduleRetry(eventId, claimToken, nextAttemptAt, errorCode) == 1;
    }

    @Transactional
    public boolean markFailed(UUID eventId, UUID claimToken, String errorCode) {
        return events.markFailed(eventId, claimToken, errorCode) == 1;
    }

    private OutboxClaim claim(OutboxEventEntity event, String instance, Instant now, Instant expiresAt) {
        var view = event.claim(UUID.randomUUID(), instance, now, expiresAt);
        var correlationId = view.correlationId() == null ? view.jobId() : view.correlationId();
        return new OutboxClaim(view.id(), view.jobId(), correlationId, view.token(), view.routingKey(),
            view.payload(), view.occurredAt(), view.attempts(), view.recovered());
    }
}
