package br.com.fiapx.api.job;

import com.fasterxml.jackson.databind.*;

import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ResultEventListener {
    private final JobRepository jobs;
    private final ObjectMapper json;

    public ResultEventListener(JobRepository j, ObjectMapper m) {
        jobs = j;
        json = m;
    }

    @RabbitListener(queues = "video.api.results.v1")
    @Transactional
    public void apply(String body) throws Exception {
        JsonNode e = json.readTree(body);
        Job job = jobs.findById(UUID.fromString(e.required("jobId").asText())).orElseThrow();
        String type = e.required("type").asText();
        job.apply(Job.Status.valueOf(type), e.path("resultKey").asText(null));
    }
}
