package br.com.fiapx.videoapi.identity.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleId> {
    boolean existsByRole(String role);
    UserRoleEntity findFirstByUserId(UUID userId);
}
