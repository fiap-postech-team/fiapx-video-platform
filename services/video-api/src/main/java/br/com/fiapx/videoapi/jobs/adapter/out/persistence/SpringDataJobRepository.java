package br.com.fiapx.videoapi.jobs.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataJobRepository extends JpaRepository<JobEntity, UUID> {
    Optional<JobEntity> findByIdAndUserId(UUID id, UUID userId);
}
