package br.com.fiapx.videoapi.videos.adapter.configuration;

import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.video")
public record VideoUploadProperties(long maxSizeBytes, Set<String> allowedContentTypes,
                                    Duration uploadUrlTtl, Duration pendingTtl, int cleanupBatchSize,
                                    String bucket, String internalEndpoint, String publicEndpoint,
                                    String region, String accessKey, String secretKey) {
    public VideoUploadProperties {
        if (bucket == null || bucket.isBlank() || region == null || region.isBlank()
            || (hasText(accessKey) != hasText(secretKey))) {
            throw new IllegalArgumentException("Invalid video storage configuration");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
