package br.com.fiapx.videoapi.outbox.adapter.out.persistence;

import java.util.UUID;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOutboxRepository extends JpaRepository<OutboxEventEntity, UUID> {
    @Query(value = "select * from outbox_events where (status = 'PENDING' and next_attempt_at <= :now) "
        + "or (status = 'PROCESSING' and claim_expires_at <= :now) order by created_at for update skip locked limit :limit", nativeQuery = true)
    List<OutboxEventEntity> lockReady(Instant now, int limit);
}
