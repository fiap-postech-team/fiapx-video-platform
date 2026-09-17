package br.com.fiapx.videoapi.identity.application.port.out;

import br.com.fiapx.videoapi.identity.domain.IdentityUser;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdentityStore {
    Optional<IdentityUser> findUserForUpdate(String email);
    Optional<IdentityUser> findUser(UUID id);
    IdentityUser saveUser(IdentityUser user);
    boolean hasAdministrator();
    void createSession(UUID sessionId, UUID userId, Instant expiresAt);
    void createRefreshToken(UUID tokenId, UUID sessionId, String hash, Instant issuedAt, Instant expiresAt);
    Optional<RefreshTokenRecord> findRefreshTokenForUpdate(String hash);
    void consumeRefreshToken(UUID tokenId, Instant consumedAt);
    default void rotateRefreshToken(UUID consumedTokenId, UUID replacementTokenId, UUID sessionId,
                                    String hash, Instant issuedAt, Instant expiresAt) {
        consumeRefreshToken(consumedTokenId, issuedAt);
        createRefreshToken(replacementTokenId, sessionId, hash, issuedAt, expiresAt);
    }
    void revokeSession(UUID sessionId, Instant revokedAt);
    boolean hasActiveSession(UUID sessionId, UUID userId, Instant now);

    record RefreshTokenRecord(UUID id, UUID sessionId, UUID userId, Instant expiresAt, Instant consumedAt,
                               Instant revokedAt) {
        public boolean canRefresh(Instant now) {
            return consumedAt == null && revokedAt == null && expiresAt.isAfter(now);
        }
    }
}
