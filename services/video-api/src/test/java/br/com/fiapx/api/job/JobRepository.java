package br.com.fiapx.api.job;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {
    Optional<Job> findByIdAndUserId(UUID id, UUID userId);

    List<Job> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<Job> findByUserId(UUID userId, Pageable pageable);

    Page<Job> findByStatus(Job.Status status, Pageable pageable);

    Page<Job> findByUserIdAndStatus(UUID userId, Job.Status status, Pageable pageable);
}
