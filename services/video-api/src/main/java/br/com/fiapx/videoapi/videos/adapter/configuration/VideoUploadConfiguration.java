package br.com.fiapx.videoapi.videos.adapter.configuration;

import br.com.fiapx.videoapi.videos.application.ConfirmVideo;
import br.com.fiapx.videoapi.videos.application.CreateVideoUpload;
import br.com.fiapx.videoapi.videos.application.ExpireVideoUploads;
import br.com.fiapx.videoapi.videos.application.VideoUploadPolicy;
import br.com.fiapx.videoapi.videos.application.port.out.VideoObjectStorage;
import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import br.com.fiapx.videoapi.videos.application.port.out.VideoTransactions;
import br.com.fiapx.videoapi.videos.adapter.out.storage.S3VideoObjectStorage;
import java.net.URI;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(VideoUploadProperties.class)
@EnableScheduling
public class VideoUploadConfiguration {
    @Bean
    VideoUploadPolicy videoUploadPolicy(VideoUploadProperties p) {
        return new VideoUploadPolicy(p.maxSizeBytes(), p.allowedContentTypes(), p.uploadUrlTtl(),
            p.pendingTtl(), p.cleanupBatchSize());
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.video", name = "storage-mode", havingValue = "s3", matchIfMissing = true)
    AwsCredentialsProvider videoStorageCredentials(VideoUploadProperties p) {
        if (p.accessKey() == null || p.accessKey().isBlank()) {
            return DefaultCredentialsProvider.create();
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(p.accessKey(), p.secretKey()));
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.video", name = "storage-mode", havingValue = "s3", matchIfMissing = true)
    S3Client videoS3Client(VideoUploadProperties p, AwsCredentialsProvider credentials) {
        var builder = S3Client.builder().region(Region.of(p.region())).credentialsProvider(credentials);
        if (p.internalEndpoint() != null && !p.internalEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(p.internalEndpoint())).forcePathStyle(true);
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.video", name = "storage-mode", havingValue = "s3", matchIfMissing = true)
    S3Presigner videoS3Presigner(VideoUploadProperties p, AwsCredentialsProvider credentials) {
        var builder = S3Presigner.builder().region(Region.of(p.region())).credentialsProvider(credentials);
        var publicEndpoint = p.publicEndpoint() == null || p.publicEndpoint().isBlank()
            ? p.internalEndpoint() : p.publicEndpoint();
        if (publicEndpoint != null && !publicEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(publicEndpoint)).serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true).checksumValidationEnabled(false).build());
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.video", name = "storage-mode", havingValue = "s3", matchIfMissing = true)
    VideoObjectStorage videoObjectStorage(S3Client client, S3Presigner presigner, VideoUploadProperties p) {
        return new S3VideoObjectStorage(client, presigner, p.bucket());
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.video", name = "storage-mode", havingValue = "mock")
    LocalMockVideoObjectStorage localMockVideoObjectStorage(
        VideoUploadProperties properties,
        Clock clock,
        @org.springframework.beans.factory.annotation.Value("${app.video.local-public-endpoint}") String publicEndpoint,
        @org.springframework.beans.factory.annotation.Value("${app.video.local-directory}") String directory
    ) {
        return new LocalMockVideoObjectStorage(properties.maxSizeBytes(), publicEndpoint, java.nio.file.Path.of(directory), clock);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.video", name = "storage-mode", havingValue = "mock")
    VideoObjectStorage videoObjectStorage(LocalMockVideoObjectStorage storage) {
        return storage;
    }

    @Bean
    CreateVideoUpload createVideoUpload(VideoStore videos, VideoObjectStorage storage,
                                        VideoTransactions transactions, VideoUploadPolicy policy, Clock clock) {
        return new CreateVideoUpload(videos, storage, transactions, policy, clock);
    }

    @Bean
    ConfirmVideo confirmVideo(VideoStore videos, VideoObjectStorage storage,
                              VideoTransactions transactions, Clock clock) {
        return new ConfirmVideo(videos, storage, transactions, clock);
    }

    @Bean
    ExpireVideoUploads expireVideoUploads(VideoStore videos, VideoObjectStorage storage,
                                          VideoTransactions transactions, VideoUploadPolicy policy, Clock clock) {
        return new ExpireVideoUploads(videos, storage, transactions, policy, clock);
    }
}
