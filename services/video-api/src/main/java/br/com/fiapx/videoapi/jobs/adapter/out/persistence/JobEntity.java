package br.com.fiapx.videoapi.jobs.adapter.out.persistence;

import br.com.fiapx.videoapi.jobs.domain.Job;
import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import br.com.fiapx.videoapi.jobs.domain.JobSourceKind;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class JobEntity {
    @Id private UUID id;
    private UUID userId;
    private UUID videoId;
    @Enumerated(EnumType.STRING) private JobSourceKind sourceKind;
    private String sourceKey;
    private String resultKey;
    @Enumerated(EnumType.STRING) private JobStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private String failureCode;
    private Instant completedAt;
    private boolean videoLibraryVisible;
    @Version private long version;
    protected JobEntity() { }
    JobEntity(Job job) {
        id = job.id(); userId = job.userId(); videoId = job.videoId(); sourceKind = job.sourceKind();
        sourceKey = job.sourceKey(); resultKey = job.resultKey(); status = job.status();
        failureCode = job.failureCode(); completedAt = job.completedAt();
        createdAt = job.createdAt(); updatedAt = createdAt; videoLibraryVisible = job.libraryVisible();
    }
    Job toDomain() {
        return new Job(id, userId, videoId, sourceKind, sourceKey, resultKey, status, createdAt,
            videoLibraryVisible, failureCode, completedAt);
    }
    void apply(Job job, Instant updatedAt) {
        status = job.status();
        resultKey = job.resultKey();
        failureCode = job.failureCode();
        completedAt = job.completedAt();
        this.updatedAt = updatedAt;
    }
}
