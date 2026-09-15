package br.com.fiapx.videoapi.outbox.application.port.out;

import java.util.UUID;

public interface OutboxStore {
    void append(UUID eventId, UUID jobId, UUID userId, String sourceKey);
}
