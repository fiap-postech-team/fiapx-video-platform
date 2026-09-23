package br.com.fiapx.videoapi.identity.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserCredentialRepository extends JpaRepository<UserCredentialEntity, UUID> {
}
