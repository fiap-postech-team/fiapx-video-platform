package br.com.fiapx.videoapi.outbox.application.port.out;

import br.com.fiapx.videoapi.outbox.domain.OutboxClaim;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxStore {
    void append(UUID eventId, UUID jobId, UUID userId, String sourceKey);
    default void append(UUID eventId, UUID jobId, UUID userId, UUID videoId, String sourceKey) {
        append(eventId, jobId, userId, sourceKey);
    }
    default List<OutboxClaim> claimReady(int limit, String instanceId, Instant now, Instant expiresAt) {
        throw new UnsupportedOperationException();
    }
    default boolean markPublished(UUID eventId, UUID claimToken, Instant publishedAt) { return false; }
    default boolean scheduleRetry(UUID eventId, UUID claimToken, Instant nextAttemptAt, String errorCode) { return false; }
}
