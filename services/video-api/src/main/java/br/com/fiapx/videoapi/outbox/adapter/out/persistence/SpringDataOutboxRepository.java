package br.com.fiapx.videoapi.outbox.adapter.out.persistence;

import java.util.UUID;
import java.time.Instant;
import java.util.List;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOutboxRepository extends JpaRepository<OutboxEventEntity, UUID> {
    @Query(value = "select * from outbox_events where (status = 'PENDING' and next_attempt_at <= :now) "
        + "or (status = 'PROCESSING' and claim_expires_at <= :now) "
        + "order by created_at limit :limit for update skip locked", nativeQuery = true)
    List<OutboxEventEntity> lockReady(@Param("now") Instant now, @Param("limit") int limit);

    @Modifying
    @Query(value = "update outbox_events set status = 'PUBLISHED', published_at = :publishedAt, "
        + "last_error_code = null, "
        + "claim_token = null, claimed_by = null, claimed_at = null, claim_expires_at = null "
        + "where id = :eventId and claim_token = :claimToken and status = 'PROCESSING'", nativeQuery = true)
    int markPublished(@Param("eventId") UUID eventId, @Param("claimToken") UUID claimToken,
                      @Param("publishedAt") Instant publishedAt);

    @Modifying
    @Query(value = "update outbox_events set status = 'PENDING', "
        + "next_attempt_at = :nextAttemptAt, last_error_code = :errorCode, "
        + "claim_token = null, claimed_by = null, claimed_at = null, claim_expires_at = null "
        + "where id = :eventId and claim_token = :claimToken and status = 'PROCESSING'", nativeQuery = true)
    int scheduleRetry(@Param("eventId") UUID eventId, @Param("claimToken") UUID claimToken,
                      @Param("nextAttemptAt") Instant nextAttemptAt, @Param("errorCode") String errorCode);

    @Modifying
    @Query(value = "update outbox_events set status = 'FAILED', "
        + "last_error_code = :errorCode, claim_token = null, claimed_by = null, claimed_at = null, "
        + "claim_expires_at = null where id = :eventId and claim_token = :claimToken "
        + "and status = 'PROCESSING'", nativeQuery = true)
    int markFailed(@Param("eventId") UUID eventId, @Param("claimToken") UUID claimToken,
                   @Param("errorCode") String errorCode);
}
