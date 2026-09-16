package br.com.fiapx.videoapi.identity.adapter.out.persistence;

import br.com.fiapx.videoapi.identity.application.port.out.IdentityStore;
import br.com.fiapx.videoapi.identity.domain.IdentityUser;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class JpaIdentityStore implements IdentityStore {
    private final IdentityUserRepository users;
    private final AuthSessionRepository sessions;
    private final RefreshTokenRepository tokens;

    public JpaIdentityStore(IdentityUserRepository users, AuthSessionRepository sessions, RefreshTokenRepository tokens) {
        this.users = users; this.sessions = sessions; this.tokens = tokens;
    }

    public Optional<IdentityUser> findUserForUpdate(String email) { return users.findByEmail(email).map(this::user); }
    public Optional<IdentityUser> findUser(UUID id) { return users.findById(id).map(this::user); }
    public IdentityUser saveUser(IdentityUser user) { return user(users.save(entity(user))); }
    public boolean hasAdministrator() { return users.existsByRole(br.com.fiapx.videoapi.identity.domain.UserRole.ADMIN); }

    public void createSession(UUID id, UUID userId, Instant expiresAt) {
        var session = new AuthSessionEntity(); session.id = id; session.userId = userId; session.expiresAt = expiresAt; sessions.save(session);
    }

    public void createRefreshToken(UUID id, UUID sessionId, String hash, Instant issuedAt, Instant expiresAt) {
        var token = new RefreshTokenEntity(); token.id = id; token.sessionId = sessionId; token.tokenHash = hash;
        token.issuedAt = issuedAt; token.expiresAt = expiresAt; tokens.save(token);
    }

    public Optional<RefreshTokenRecord> findRefreshTokenForUpdate(String hash) {
        return tokens.findByTokenHash(hash).flatMap(token -> sessions.findById(token.sessionId).map(session -> record(token, session)));
    }

    public void consumeRefreshToken(UUID id, Instant consumedAt) { tokens.findById(id).ifPresent(token -> token.consumedAt = consumedAt); }
    public void revokeSession(UUID id, Instant revokedAt) { sessions.findById(id).ifPresent(session -> session.revokedAt = revokedAt); }
    public boolean hasActiveSession(UUID id, UUID userId, Instant now) { return users.hasActiveSession(id, userId, now); }

    private RefreshTokenRecord record(RefreshTokenEntity token, AuthSessionEntity session) {
        return new RefreshTokenRecord(token.id, session.id, session.userId, token.expiresAt, token.consumedAt, session.revokedAt);
    }

    private IdentityUser user(IdentityUserEntity entity) {
        return new IdentityUser(entity.id, entity.email, entity.passwordHash, entity.role, entity.status, entity.failedAttempts, entity.lockedUntil);
    }

    private IdentityUserEntity entity(IdentityUser user) {
        var entity = new IdentityUserEntity(); entity.id = user.id(); entity.email = user.email(); entity.passwordHash = user.passwordHash();
        entity.role = user.role(); entity.status = user.status(); entity.failedAttempts = user.failedAttempts(); entity.lockedUntil = user.lockedUntil(); return entity;
    }
}
