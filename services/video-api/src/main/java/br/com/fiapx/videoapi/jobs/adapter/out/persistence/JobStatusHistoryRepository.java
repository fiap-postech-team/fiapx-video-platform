package br.com.fiapx.videoapi.jobs.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface JobStatusHistoryRepository extends JpaRepository<JobStatusHistoryEntity, Long> {
}
