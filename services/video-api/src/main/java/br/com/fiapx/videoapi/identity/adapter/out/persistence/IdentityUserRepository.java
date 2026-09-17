package br.com.fiapx.videoapi.identity.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface IdentityUserRepository extends JpaRepository<IdentityUserEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<IdentityUserEntity> findByEmail(String email);

    @Query("select count(s) > 0 from AuthSessionEntity s where s.id = :sessionId and s.userId = :userId "
        + "and s.revokedAt is null and s.expiresAt > :now")
    boolean hasActiveSession(UUID sessionId, UUID userId, java.time.Instant now);
}
