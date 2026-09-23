package br.com.fiapx.videoapi.jobs.adapter.configuration;

import br.com.fiapx.videoapi.identity.application.RegisterUser;
import br.com.fiapx.videoapi.identity.application.port.out.IdentityStore;
import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.domain.Job;
import br.com.fiapx.videoapi.jobs.domain.JobSourceKind;
import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import br.com.fiapx.videoapi.videos.adapter.configuration.LocalMockVideoObjectStorage;
import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import br.com.fiapx.videoapi.videos.domain.Video;
import br.com.fiapx.videoapi.videos.domain.VideoStatus;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("local")
@ConditionalOnProperty(prefix = "app.video", name = "storage-mode", havingValue = "mock")
@EnableConfigurationProperties(DownloadFixtureProperties.class)
public class LocalDownloadFixture {
    @Bean
    @ConditionalOnProperty(prefix = "app.video.download-fixture", name = "enabled", havingValue = "true")
    ApplicationRunner downloadFixtureRunner(DownloadFixtureProperties properties, IdentityStore identities,
                                           RegisterUser registerUser, VideoStore videos, JobStore jobs,
                                           LocalMockVideoObjectStorage storage, Clock clock,
                                           TransactionTemplate transactions) {
        return arguments -> seed(properties, identities, registerUser, videos, jobs, storage, clock, transactions);
    }

    private void seed(DownloadFixtureProperties properties, IdentityStore identities, RegisterUser registerUser,
                      VideoStore videos, JobStore jobs, LocalMockVideoObjectStorage storage, Clock clock,
                      TransactionTemplate transactions) {
        if (properties.email() == null || properties.password() == null
            || properties.email().isBlank() || properties.password().length() < 12) {
            throw new IllegalStateException("Download fixture requires APP_VIDEO_DOWNLOAD_FIXTURE_EMAIL and a password of at least 12 characters");
        }
        var user = transactions.execute(status -> identities.findUserForUpdate(RegisterUser.normalize(properties.email()))
            .orElseGet(() -> registerUser.execute(properties.email(), properties.password())));
        var now = clock.instant();
        var videoId = properties.videoId() == null ? UUID.fromString("00000000-0000-4000-8000-000000000101") : properties.videoId();
        var jobId = properties.jobId() == null ? UUID.fromString("00000000-0000-4000-8000-000000000102") : properties.jobId();
        var sourceKey = "users/" + user.id() + "/videos/" + videoId + "/source";
        var resultKey = "results/" + jobId + "/frames.zip";
        var seededResultKey = transactions.execute(status -> {
            if (videos.findOwnedById(user.id(), videoId).isEmpty()) {
                videos.save(new Video(videoId, user.id(), sourceKey, "demo-processado.mp4", "video/mp4", 1,
                    null, VideoStatus.UPLOADED, now.minus(Duration.ofMinutes(2)), now.minus(Duration.ofMinutes(1)),
                    now.plus(Duration.ofHours(24)), now.minus(Duration.ofMinutes(1)), null));
            }
            var existing = jobs.findVisibleByVideoId(videoId);
            if (existing.isEmpty()) {
                jobs.save(new Job(jobId, user.id(), videoId, JobSourceKind.VIDEO, sourceKey, resultKey,
                    JobStatus.COMPLETED, now.minus(Duration.ofMinutes(1)), true));
                return resultKey;
            } else if (existing.get().status() == JobStatus.PENDING || existing.get().status() == JobStatus.PROCESSING) {
                existing.get().apply(JobStatus.COMPLETED, resultKey);
                jobs.save(existing.get());
                return resultKey;
            }
            return existing.get().resultKey() == null || existing.get().resultKey().isBlank()
                ? resultKey : existing.get().resultKey();
        });
        storage.seed(seededResultKey, zipBytes(), "application/zip");
    }

    private byte[] zipBytes() {
        try {
            var output = new ByteArrayOutputStream();
            try (var zip = new ZipOutputStream(output)) {
                zip.putNextEntry(new ZipEntry("README.txt"));
                zip.write("FIAP X local result\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create local result ZIP", exception);
        }
    }
}
