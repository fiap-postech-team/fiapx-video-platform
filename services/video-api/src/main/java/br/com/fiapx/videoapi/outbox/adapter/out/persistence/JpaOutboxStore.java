package br.com.fiapx.videoapi.outbox.adapter.out.persistence;

import br.com.fiapx.videoapi.outbox.application.port.out.OutboxStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

@Component
public final class JpaOutboxStore implements OutboxStore {
    private final Events events;
    private final ObjectMapper json;
    public JpaOutboxStore(Events events, ObjectMapper json) { this.events = events; this.json = json; }
    public void append(UUID eventId, UUID jobId, UUID userId, String sourceKey) {
        try { events.save(new OutboxEventEntity(eventId, json.writeValueAsString(Map.of("eventId", eventId, "jobId", jobId, "userId", userId, "sourceKey", sourceKey)))); }
        catch (Exception exception) { throw new IllegalStateException("Cannot serialize job event", exception); }
    }
    interface Events extends JpaRepository<OutboxEventEntity, UUID> { }
}
