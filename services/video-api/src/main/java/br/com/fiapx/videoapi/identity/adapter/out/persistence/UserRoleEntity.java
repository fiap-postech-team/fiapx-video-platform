package br.com.fiapx.videoapi.identity.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "user_roles")
@IdClass(UserRoleId.class)
class UserRoleEntity {
    @Id UUID userId;
    @Id String role;

    protected UserRoleEntity() {
    }
}
