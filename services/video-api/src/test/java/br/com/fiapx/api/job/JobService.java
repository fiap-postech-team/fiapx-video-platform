package br.com.fiapx.api.job;

import br.com.fiapx.api.outbox.OutboxEvent;
import br.com.fiapx.api.outbox.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JobService {
    private final JobRepository jobs;
    private final OutboxRepository outbox;
    private final ObjectMapper json;

    public JobService(JobRepository jobs, OutboxRepository outbox, ObjectMapper json) {
        this.jobs = jobs;
        this.outbox = outbox;
        this.json = json;
    }

    public Job create(UUID userId, String sourceKey) {
        Job job = jobs.save(new Job(userId, sourceKey));
        outbox.save(new OutboxEvent("video.job.requested.v1", serializeEvent(job, userId, sourceKey)));
        return job;
    }

    public Job getOwned(UUID jobId, UUID userId) {
        return jobs.findByIdAndUserId(jobId, userId).orElseThrow(() -> new JobNotFoundException(jobId));
    }

    public List<Job> listOwned(UUID userId) {
        return jobs.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    public Page<Job> search(UUID userId, Job.Status status, Pageable pageable) {
        if (userId != null && status != null) {
            return jobs.findByUserIdAndStatus(userId, status, pageable);
        }
        if (userId != null) {
            return jobs.findByUserId(userId, pageable);
        }
        if (status != null) {
            return jobs.findByStatus(status, pageable);
        }
        return jobs.findAll(pageable);
    }

    public Job getAny(UUID jobId) {
        return jobs.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
    }

    private String serializeEvent(Job job, UUID userId, String sourceKey) {
        try {
            return json.writeValueAsString(Map.of(
                    "eventId", UUID.randomUUID(),
                    "jobId", job.getId(),
                    "userId", userId,
                    "sourceKey", sourceKey
            ));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize event", ex);
        }
    }
}
