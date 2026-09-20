package br.com.fiapx.videoapi.videos.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VideoUploadPolicyTest {
    private static final Duration URL_TTL = Duration.ofMinutes(15);
    private static final Duration PENDING_TTL = Duration.ofHours(24);

    @Test
    void acceptsDeclaredMediaWithOrWithoutChecksum() {
        var policy = policy();
        assertThatCode(() -> policy.validate("clip.mp4", "video/mp4", 500_000_000, null))
            .doesNotThrowAnyException();
        assertThatCode(() -> policy.validate("clip.mp4", "video/mp4", 1, "a".repeat(64)))
            .doesNotThrowAnyException();
    }

    @Test
    void rejectsUnsafeNamesAndInvalidMediaDeclarations() {
        var policy = policy();
        for (String name : new String[] {null, "", " ", "x".repeat(513), "a/b", "a\\b", "a\nb"}) {
            assertThatThrownBy(() -> policy.validate(name, "video/mp4", 1, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> policy.validate("clip", "text/plain", 1, null))
            .isInstanceOf(IllegalArgumentException.class);
        for (long size : new long[] {0, -1, 500_000_001}) {
            assertThatThrownBy(() -> policy.validate("clip", "video/mp4", size, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> policy.validate("clip", "video/mp4", 1, "not-a-sha256"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidPolicyConfiguration() {
        assertInvalid(0, Set.of("video/mp4"), URL_TTL, PENDING_TTL, 10);
        assertInvalid(1, null, URL_TTL, PENDING_TTL, 10);
        assertInvalid(1, Set.of(), URL_TTL, PENDING_TTL, 10);
        assertInvalid(1, Set.of("video/mp4"), Duration.ZERO, PENDING_TTL, 10);
        assertInvalid(1, Set.of("video/mp4"), Duration.ofSeconds(-1), PENDING_TTL, 10);
        assertInvalid(1, Set.of("video/mp4"), URL_TTL, URL_TTL, 10);
        assertInvalid(1, Set.of("video/mp4"), URL_TTL, Duration.ofMinutes(1), 10);
        assertInvalid(1, Set.of("video/mp4"), URL_TTL, PENDING_TTL, 0);
    }

    private VideoUploadPolicy policy() {
        return new VideoUploadPolicy(500_000_000, Set.of("video/mp4"), URL_TTL, PENDING_TTL, 10);
    }

    private void assertInvalid(long max, Set<String> types, Duration urlTtl, Duration pendingTtl, int batch) {
        assertThatThrownBy(() -> new VideoUploadPolicy(max, types, urlTtl, pendingTtl, batch))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
