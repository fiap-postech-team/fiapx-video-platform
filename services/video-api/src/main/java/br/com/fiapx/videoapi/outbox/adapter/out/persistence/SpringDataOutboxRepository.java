package br.com.fiapx.videoapi.outbox.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOutboxRepository extends JpaRepository<OutboxEventEntity, UUID> { }
