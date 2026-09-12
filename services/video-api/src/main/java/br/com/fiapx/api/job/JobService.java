package br.com.fiapx.api.job;

import br.com.fiapx.api.outbox.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {
    private final JobRepository jobs;
    private final OutboxRepository outbox;
    private final ObjectMapper json;

    public JobService(JobRepository j, OutboxRepository o, ObjectMapper m) {
        jobs = j;
        outbox = o;
        json = m;
    }

    @Transactional
    public Job create(UUID userId, String sourceKey) {
        Job job = jobs.save(new Job(userId, sourceKey));
        try {
            outbox.save(new OutboxEvent("video.job.requested.v1", json.writeValueAsString(Map.of("eventId", UUID.randomUUID(), "jobId", job.getId(), "userId", userId, "sourceKey", sourceKey))));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize event", e);
        }
        return job;
    }

    public Job get(UUID id) {
        return jobs.findById(id).orElseThrow(() -> new NoSuchElementException("Job not found"));
    }
}
