package br.com.fiapx.videoprocessor.processing.application.port.out;

import br.com.fiapx.videoprocessor.processing.domain.JobEvent;

public interface JobEventPublisher {

    void publish(JobEvent event);
}
