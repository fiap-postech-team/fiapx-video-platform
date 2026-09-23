package br.com.fiapx.videoapi.jobs.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface JobCreationIdempotencyRepository extends JpaRepository<JobCreationIdempotencyEntity, JobCreationIdempotencyId> {
    @Query(value = "select pg_advisory_xact_lock(hashtextextended(:lockKey, 0))", nativeQuery = true)
    void lock(@Param("lockKey") String lockKey);

    Optional<JobCreationIdempotencyEntity> findByIdUserIdAndIdIdempotencyKey(UUID userId, String key);
}
