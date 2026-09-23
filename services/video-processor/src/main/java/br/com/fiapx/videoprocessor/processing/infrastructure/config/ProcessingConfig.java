package br.com.fiapx.videoprocessor.processing.infrastructure.config;

import br.com.fiapx.videoprocessor.processing.application.ProcessVideoJobService;
import br.com.fiapx.videoprocessor.processing.application.port.in.ProcessVideoJob;
import br.com.fiapx.videoprocessor.processing.application.port.out.FrameArchiver;
import br.com.fiapx.videoprocessor.processing.application.port.out.FrameExtractor;
import br.com.fiapx.videoprocessor.processing.application.port.out.JobEventPublisher;
import br.com.fiapx.videoprocessor.processing.application.port.out.MediaInspector;
import br.com.fiapx.videoprocessor.processing.application.port.out.VideoObjectStorage;
import br.com.fiapx.videoprocessor.processing.application.port.out.WorkspaceFactory;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the use case here so the application layer stays free of framework annotations.
 */
@Configuration
class ProcessingConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ProcessVideoJob processVideoJob(
            VideoObjectStorage storage,
            MediaInspector inspector,
            FrameExtractor frameExtractor,
            FrameArchiver archiver,
            JobEventPublisher publisher,
            WorkspaceFactory workspaces,
            Clock clock) {
        return new ProcessVideoJobService(storage, inspector, frameExtractor, archiver, publisher, workspaces, clock);
    }
}
