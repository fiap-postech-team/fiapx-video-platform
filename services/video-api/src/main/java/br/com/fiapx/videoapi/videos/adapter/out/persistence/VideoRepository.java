package br.com.fiapx.videoapi.videos.adapter.out.persistence;

import br.com.fiapx.videoapi.videos.domain.VideoStatus;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.time.Instant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

interface VideoRepository extends JpaRepository<VideoEntity, UUID> {
    Optional<VideoEntity> findByUserIdAndObjectKey(UUID userId, String objectKey);
    Optional<VideoEntity> findByUserIdAndObjectKeyAndUploadStatus(UUID userId, String objectKey, VideoStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VideoEntity> findByIdAndUserId(UUID id, UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VideoEntity v where v.id = :id")
    Optional<VideoEntity> lockById(@Param("id") UUID id);
    @Query(value = "select id from videos where upload_status = 'PENDING' and expires_at <= :now "
        + "order by expires_at, id limit :limit", nativeQuery = true)
    List<UUID> pendingExpired(@Param("now") Instant now, @Param("limit") int limit);
    @Query(value = "select id from videos where upload_status = 'EXPIRED' and cleanup_completed_at is null "
        + "order by expires_at, id limit :limit", nativeQuery = true)
    List<UUID> expiredUncleaned(@Param("limit") int limit);
}
