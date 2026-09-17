package br.com.fiapx.videoapi.inbox.adapter.out.persistence;

import br.com.fiapx.videoapi.inbox.application.port.out.InboxStore;
import br.com.fiapx.videoapi.inbox.domain.JobResultEvent;
import org.springframework.stereotype.Component;

@Component
public final class JpaInboxStore implements InboxStore {
    private final InboxEventRepository events;

    public JpaInboxStore(InboxEventRepository events) {
        this.events = events;
    }

    public boolean register(JobResultEvent event) {
        var now = java.time.Instant.now();
        return events.insertIfAbsent(event.eventId(), event.jobId(), event.status().name(), event.jobId(),
                event.fingerprint(), event.occurredAt(), now) == 1;
    }
}
