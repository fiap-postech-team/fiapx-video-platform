package br.com.fiapx.videoapi.videos.adapter.configuration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VideoUploadPropertiesTest {
    @Test
    void allowsDefaultCredentialsOrAnExplicitPair() {
        assertThatCode(() -> properties("videos", "us-east-1", null, null)).doesNotThrowAnyException();
        assertThatCode(() -> properties("videos", "us-east-1", "key", "secret"))
            .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingBucketRegionOrHalfOfCredentialPair() {
        assertInvalid(null, "us-east-1", null, null);
        assertInvalid(" ", "us-east-1", null, null);
        assertInvalid("videos", null, null, null);
        assertInvalid("videos", " ", null, null);
        assertInvalid("videos", "us-east-1", "key", null);
        assertInvalid("videos", "us-east-1", null, "secret");
        assertInvalid("videos", "us-east-1", " ", "secret");
    }

    private void assertInvalid(String bucket, String region, String key, String secret) {
        assertThatThrownBy(() -> properties(bucket, region, key, secret))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private VideoUploadProperties properties(String bucket, String region, String key, String secret) {
        return new VideoUploadProperties(500_000_000, Set.of("video/mp4"), Duration.ofMinutes(15),
            Duration.ofHours(24), 100, bucket, null, null, region, key, secret);
    }
}
