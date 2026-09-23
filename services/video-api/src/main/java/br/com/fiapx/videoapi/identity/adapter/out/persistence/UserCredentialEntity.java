package br.com.fiapx.videoapi.identity.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_credentials")
class UserCredentialEntity {
    @Id UUID userId;
    String passwordHash;
    String passwordAlgorithm;
    Instant passwordChangedAt;
    int failedAttempts;
    Instant lockedUntil;

    protected UserCredentialEntity() {
    }
}
