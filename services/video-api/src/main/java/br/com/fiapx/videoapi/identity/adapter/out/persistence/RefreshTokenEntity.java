package br.com.fiapx.videoapi.identity.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity {
    @Id UUID id;
    UUID sessionId;
    String tokenHash;
    Instant issuedAt;
    Instant expiresAt;
    Instant consumedAt;

    protected RefreshTokenEntity() {
    }
}
