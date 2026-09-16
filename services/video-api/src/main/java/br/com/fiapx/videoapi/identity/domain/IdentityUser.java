package br.com.fiapx.videoapi.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record IdentityUser(
    UUID id,
    String email,
    String passwordHash,
    UserRole role,
    UserStatus status,
    int failedAttempts,
    Instant lockedUntil
) {
    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public IdentityUser failed(Instant lockUntil) {
        return new IdentityUser(id, email, passwordHash, role, status, failedAttempts + 1, lockUntil);
    }

    public IdentityUser clearedFailures() {
        return new IdentityUser(id, email, passwordHash, role, status, 0, null);
    }
}
