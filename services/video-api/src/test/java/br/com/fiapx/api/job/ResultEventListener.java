package br.com.fiapx.api.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ResultEventListener {
    private final JobRepository jobs;
    private final ObjectMapper json;

    public ResultEventListener(JobRepository jobs, ObjectMapper json) {
        this.jobs = jobs;
        this.json = json;
    }

    public void apply(String payload) throws Exception {
        JsonNode root = json.readTree(payload);
        Job job = jobs.findById(UUID.fromString(root.required("jobId").asText())).orElseThrow();
        job.apply(Job.Status.valueOf(root.required("type").asText()), root.path("resultKey").asText(null));
    }
}
