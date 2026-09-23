package br.com.fiapx.videoapi.identity.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
class AuthSessionEntity {
    @Id UUID id;
    UUID userId;
    Instant expiresAt;
    Instant revokedAt;
    Instant createdAt;
    Instant lastUsedAt;
    String revocationReason;

    protected AuthSessionEntity() {
    }
}
