package br.com.fiapx.videoapi.identity.adapter.out.persistence;

import br.com.fiapx.videoapi.identity.domain.UserStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@Table(name = "users")
class IdentityUserEntity {
    @Id UUID id;
    String email;
    @Enumerated(EnumType.STRING) UserStatus status;

    protected IdentityUserEntity() {
    }
}
