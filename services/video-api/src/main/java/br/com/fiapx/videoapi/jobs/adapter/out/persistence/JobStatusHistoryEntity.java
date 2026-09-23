package br.com.fiapx.videoapi.jobs.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_status_history")
class JobStatusHistoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    UUID jobId;
    String status;
    UUID eventId;
    String reasonCode;
    Instant occurredAt;
    Instant recordedAt;

    protected JobStatusHistoryEntity() {
    }

    JobStatusHistoryEntity(UUID jobId, String status, UUID eventId, String reasonCode,
                           Instant occurredAt, Instant recordedAt) {
        this.jobId = jobId; this.status = status; this.eventId = eventId; this.reasonCode = reasonCode;
        this.occurredAt = occurredAt; this.recordedAt = recordedAt;
    }
}
