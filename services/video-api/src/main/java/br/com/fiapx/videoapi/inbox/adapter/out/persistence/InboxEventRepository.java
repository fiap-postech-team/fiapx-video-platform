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
            values (:eventId, :jobId, :eventType, :version, :correlationId, :fingerprint,
                'RECEIVED', :occurredAt, :receivedAt, null)
            on conflict (event_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("eventId") UUID eventId, @Param("jobId") UUID jobId,
                       @Param("eventType") String eventType, @Param("correlationId") UUID correlationId,
                       @Param("fingerprint") String fingerprint, @Param("version") int version, @Param("occurredAt") java.time.Instant occurredAt,
                       @Param("receivedAt") java.time.Instant receivedAt);

    @Modifying
    @Query(value = "update inbox_events set status = :status, ignored_reason = :reason, processed_at = :processedAt where event_id = :eventId", nativeQuery = true)
    int complete(@Param("eventId") UUID eventId, @Param("status") String status,
                 @Param("reason") String reason, @Param("processedAt") java.time.Instant processedAt);
}
