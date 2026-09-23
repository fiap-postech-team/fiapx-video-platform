package br.com.fiapx.videoapi.videos.adapter.configuration;

import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "app.video")
public record VideoUploadProperties(long maxSizeBytes, Set<String> allowedContentTypes,
                                    Duration uploadUrlTtl, Duration pendingTtl, int cleanupBatchSize,
                                    String bucket, String internalEndpoint, String publicEndpoint,
                                    String region, String accessKey, String secretKey,
                                    Duration resultDownloadUrlTtl, String resultDownloadMockUrl) {
    @ConstructorBinding
    public VideoUploadProperties {
        if (bucket == null || bucket.isBlank() || region == null || region.isBlank()
            || (hasText(accessKey) != hasText(secretKey))) {
            throw new IllegalArgumentException("Invalid video storage configuration");
        }
    }

    public VideoUploadProperties(long maxSizeBytes, Set<String> allowedContentTypes,
                                 Duration uploadUrlTtl, Duration pendingTtl, int cleanupBatchSize,
                                 String bucket, String internalEndpoint, String publicEndpoint,
                                 String region, String accessKey, String secretKey) {
        this(maxSizeBytes, allowedContentTypes, uploadUrlTtl, pendingTtl, cleanupBatchSize,
            bucket, internalEndpoint, publicEndpoint, region, accessKey, secretKey,
            Duration.ofMinutes(5), null);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
