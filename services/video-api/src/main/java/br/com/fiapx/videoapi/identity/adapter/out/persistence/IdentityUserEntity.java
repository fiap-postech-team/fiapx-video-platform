package br.com.fiapx.videoapi.identity.adapter.out.persistence;

import br.com.fiapx.videoapi.identity.domain.UserRole;
import br.com.fiapx.videoapi.identity.domain.UserStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "identity_users")
class IdentityUserEntity {
    @Id UUID id;
    String email;
    String passwordHash;
    @Enumerated(EnumType.STRING) UserRole role;
    @Enumerated(EnumType.STRING) UserStatus status;
    int failedAttempts;
    Instant lockedUntil;

    protected IdentityUserEntity() {
    }
}
