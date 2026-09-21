package br.com.fiapx.videoapi.jobs.adapter.out.persistence;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

interface SpringDataJobRepository extends JpaRepository<JobEntity, UUID> {
    Optional<JobEntity> findByIdAndUserId(UUID id, UUID userId);
    Optional<JobEntity> findByUserIdAndVideoIdAndSourceKind(UUID userId, UUID videoId, br.com.fiapx.videoapi.jobs.domain.JobSourceKind sourceKind);
    List<JobEntity> findTop25ByStatusInOrderByCreatedAtAsc(List<br.com.fiapx.videoapi.jobs.domain.JobStatus> statuses);
    @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<JobEntity> findById(UUID id);
    @Query(value = """
            select * from jobs
            where user_id = :userId and (:createdBefore is null
              or (created_at, id) < (:createdBefore, :idBefore))
            order by created_at desc, id desc limit :limit
            """, nativeQuery = true)
    List<JobEntity> findOwnedPage(@Param("userId") UUID userId,
                                  @Param("createdBefore") Instant createdBefore,
                                  @Param("idBefore") UUID idBefore, @Param("limit") int limit);
}
