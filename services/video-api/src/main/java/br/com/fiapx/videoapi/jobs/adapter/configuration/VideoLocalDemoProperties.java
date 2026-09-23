package br.com.fiapx.videoapi.jobs.adapter.configuration;

import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.video")
public record VideoLocalDemoProperties(JobStatus localResultSimulatorResult) {
    public JobStatus resultStatus() {
        return localResultSimulatorResult == JobStatus.FAILED ? JobStatus.FAILED : JobStatus.COMPLETED;
    }
}
