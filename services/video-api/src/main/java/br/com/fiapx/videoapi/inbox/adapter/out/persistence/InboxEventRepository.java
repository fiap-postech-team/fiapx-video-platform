package br.com.fiapx.videoapi.inbox.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InboxEventRepository extends JpaRepository<InboxEventEntity, UUID> {
    @Modifying
    @Query(value = """
            insert into inbox_events (event_id, job_id, event_type, schema_version, correlation_id,
                payload_fingerprint, status, occurred_at, received_at, processed_at)
            values (:eventId, :jobId, :eventType, 1, :correlationId, :fingerprint,
                'PROCESSED', :occurredAt, :receivedAt, :receivedAt)
            on conflict (event_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("eventId") UUID eventId, @Param("jobId") UUID jobId,
                       @Param("eventType") String eventType, @Param("correlationId") UUID correlationId,
                       @Param("fingerprint") String fingerprint, @Param("occurredAt") java.time.Instant occurredAt,
                       @Param("receivedAt") java.time.Instant receivedAt);
}
