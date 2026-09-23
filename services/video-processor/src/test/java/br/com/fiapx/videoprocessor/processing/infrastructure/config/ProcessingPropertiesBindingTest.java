package br.com.fiapx.videoprocessor.processing.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiapx.videoprocessor.processing.domain.JobEventType;
import br.com.fiapx.videoprocessor.processing.infrastructure.media.MediaProperties;
import br.com.fiapx.videoprocessor.processing.infrastructure.messaging.MessagingProperties;
import br.com.fiapx.videoprocessor.processing.infrastructure.storage.StorageProperties;
import br.com.fiapx.videoprocessor.processing.infrastructure.workspace.WorkspaceProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Binding the defaults in {@code application.yml} is startup behaviour that no unit test reaches: a
 * renamed key or a nested record that does not bind only fails when the worker boots.
 */
class ProcessingPropertiesBindingTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(BoundProperties.class);

    @Test
    void bindsTheTopologyDeclaredByTheEventCatalog() {
        runner.run(context -> {
            MessagingProperties properties = context.getBean(MessagingProperties.class);
            assertThat(properties.exchange()).isEqualTo("video.events");
            assertThat(properties.queue()).isEqualTo("video.processing.v1");
            assertThat(properties.deadLetterQueue()).isEqualTo("video.processing.dlq.v1");
            assertThat(properties.routingKeys().requested()).isEqualTo("video.job.requested.v1");
            assertThat(properties.routingKeys().forEventType(JobEventType.PROCESSING))
                    .isEqualTo("video.job.started.v1");
            assertThat(properties.routingKeys().forEventType(JobEventType.COMPLETED))
                    .isEqualTo("video.job.completed.v1");
            assertThat(properties.routingKeys().forEventType(JobEventType.FAILED))
                    .isEqualTo("video.job.failed.v1");
        });
    }

    @Test
    void bindsTheMediaDefaultsIncludingDurations() {
        runner.run(context -> {
            MediaProperties properties = context.getBean(MediaProperties.class);
            assertThat(properties.ffmpegPath()).isEqualTo("ffmpeg");
            assertThat(properties.ffprobePath()).isEqualTo("ffprobe");
            assertThat(properties.framesPerSecond()).isEqualTo(1);
            assertThat(properties.maxDuration()).isEqualTo(Duration.ofMinutes(30));
            assertThat(properties.commandTimeout()).isEqualTo(Duration.ofMinutes(5));
        });
    }

    @Test
    void bindsTheLocalStorageAndWorkspaceDefaults() {
        runner.run(context -> {
            StorageProperties storage = context.getBean(StorageProperties.class);
            assertThat(storage.endpoint()).isEqualTo("http://localhost:9000");
            assertThat(storage.bucket()).isEqualTo("videos");
            assertThat(context.getBean(WorkspaceProperties.class).root()).isNotNull();
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({
        MessagingProperties.class,
        MediaProperties.class,
        StorageProperties.class,
        WorkspaceProperties.class
    })
    static class BoundProperties {}
}
