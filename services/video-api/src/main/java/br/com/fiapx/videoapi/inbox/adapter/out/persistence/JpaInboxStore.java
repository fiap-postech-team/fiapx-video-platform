package br.com.fiapx.videoapi.inbox.adapter.out.persistence;

import br.com.fiapx.videoapi.inbox.application.port.out.InboxStore;
import br.com.fiapx.videoapi.inbox.domain.JobResultEvent;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class JpaInboxStore implements InboxStore {
    private final InboxEventRepository events;
    private final Clock clock;

    public JpaInboxStore(InboxEventRepository events) {
        this(events, Clock.systemUTC());
    }

    @Autowired
    public JpaInboxStore(InboxEventRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    public boolean register(JobResultEvent event) {
        var now = clock.instant();
        return events.insertIfAbsent(event.eventId(), event.jobId(), event.routingKey(), event.correlationId(),
                event.fingerprint(), event.schemaVersion(), event.occurredAt(), now) == 1;
    }

    public void complete(JobResultEvent event, String reason) {
        events.complete(event.eventId(), reason == null ? "PROCESSED" : "IGNORED", reason, clock.instant());
    }
}
