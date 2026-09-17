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
    private final UserCredentialRepository credentials;
    private final UserRoleRepository roles;
    private final AuthSessionRepository sessions;
    private final RefreshTokenRepository tokens;

    public JpaIdentityStore(IdentityUserRepository users, UserCredentialRepository credentials, UserRoleRepository roles,
                            AuthSessionRepository sessions, RefreshTokenRepository tokens) {
        this.users = users; this.credentials = credentials; this.roles = roles; this.sessions = sessions; this.tokens = tokens;
    }

    public Optional<IdentityUser> findUserForUpdate(String email) { return users.findByEmail(email).map(this::user); }
    public Optional<IdentityUser> findUser(UUID id) { return users.findById(id).map(this::user); }
    public IdentityUser saveUser(IdentityUser user) {
        var saved = users.save(entity(user));
        saveCredential(user);
        saveRole(user);
        return user(saved);
    }
    public boolean hasAdministrator() { return roles.existsByRole("ADMIN"); }

    public void createSession(UUID id, UUID userId, Instant expiresAt) {
        var session = new AuthSessionEntity(); session.id = id; session.userId = userId; session.expiresAt = expiresAt;
        session.createdAt = Instant.now(); sessions.save(session);
    }

    public void createRefreshToken(UUID id, UUID sessionId, String hash, Instant issuedAt, Instant expiresAt) {
        var token = new RefreshTokenEntity(); token.id = id; token.sessionId = sessionId; token.tokenHash = hash;
        token.issuedAt = issuedAt; token.expiresAt = expiresAt; tokens.save(token);
    }

    public void rotateRefreshToken(UUID consumedId, UUID replacementId, UUID sessionId,
                                   String hash, Instant issuedAt, Instant expiresAt) {
        tokens.findById(consumedId).ifPresent(token -> {
            token.consumedAt = issuedAt;
            token.replacedById = replacementId;
        });
        sessions.findById(sessionId).ifPresent(session -> session.lastUsedAt = issuedAt);
        createRefreshToken(replacementId, sessionId, hash, issuedAt, expiresAt);
    }

    public Optional<RefreshTokenRecord> findRefreshTokenForUpdate(String hash) {
        return tokens.findByTokenHash(hash).flatMap(token -> sessions.findById(token.sessionId).map(session -> record(token, session)));
    }

    public void consumeRefreshToken(UUID id, Instant consumedAt) { tokens.findById(id).ifPresent(token -> token.consumedAt = consumedAt); }
    public void revokeSession(UUID id, Instant revokedAt) {
        sessions.findById(id).ifPresent(session -> {
            session.revokedAt = revokedAt;
            session.revocationReason = "REVOKED";
        });
    }
    public boolean hasActiveSession(UUID id, UUID userId, Instant now) { return users.hasActiveSession(id, userId, now); }

    private RefreshTokenRecord record(RefreshTokenEntity token, AuthSessionEntity session) {
        return new RefreshTokenRecord(token.id, session.id, session.userId, token.expiresAt, token.consumedAt, session.revokedAt);
    }

    private IdentityUser user(IdentityUserEntity entity) {
        var credential = credentials.findById(entity.id).orElseThrow();
        var role = roles.findFirstByUserId(entity.id);
        return new IdentityUser(entity.id, entity.email, credential.passwordHash,
            br.com.fiapx.videoapi.identity.domain.UserRole.valueOf(role.role), entity.status,
            credential.failedAttempts, credential.lockedUntil);
    }

    private IdentityUserEntity entity(IdentityUser user) {
        var entity = new IdentityUserEntity(); entity.id = user.id(); entity.email = user.email(); entity.status = user.status(); return entity;
    }

    private void saveCredential(IdentityUser user) {
        var credential = credentials.findById(user.id()).orElseGet(() -> {
            var created = new UserCredentialEntity();
            created.userId = user.id();
            created.passwordChangedAt = Instant.now();
            return created;
        });
        credential.passwordHash = user.passwordHash();
        credential.passwordAlgorithm = algorithm(user.passwordHash());
        credential.failedAttempts = user.failedAttempts();
        credential.lockedUntil = user.lockedUntil();
        credentials.save(credential);
    }

    private void saveRole(IdentityUser user) {
        var role = new UserRoleEntity(); role.userId = user.id(); role.role = user.role().name(); roles.save(role);
    }

    private String algorithm(String hash) { return hash.startsWith("{") ? hash.substring(1, hash.indexOf('}')) : "bcrypt"; }
}
