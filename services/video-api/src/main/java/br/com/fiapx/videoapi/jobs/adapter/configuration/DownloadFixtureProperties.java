package br.com.fiapx.videoapi.jobs.adapter.configuration;

import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.video.download-fixture")
public record DownloadFixtureProperties(boolean enabled, String email, String password,
                                        UUID videoId, UUID jobId) {
}
