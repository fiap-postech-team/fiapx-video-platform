package br.com.fiapx.videoapi.videos.adapter.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalMockVideoObjectStorageTest {
    @TempDir java.nio.file.Path directory;

    @Test
    void storesAStreamAndExposesObservedMetadata() {
        var storage = storage(Instant.parse("2026-09-21T12:00:00Z"));
        var signed = storage.signUpload("users/a/videos/b/source", "video/mp4", null, java.time.Duration.ofMinutes(1));
        var capability = signed.url().substring(signed.url().lastIndexOf('/') + 1);

        storage.put(capability, "video/mp4", "*", null, new ByteArrayInputStream("video".getBytes()));

        var object = storage.stat("users/a/videos/b/source").orElseThrow();
        assertThat(object.sizeBytes()).isEqualTo(5);
        assertThat(object.contentType()).isEqualTo("video/mp4");
    }

    @Test
    void rejectsExpiredOrTooLargeUploads() {
        var storage = new LocalMockVideoObjectStorage(3, "http://localhost:3001", directory,
            Clock.fixed(Instant.parse("2026-09-21T12:00:00Z"), ZoneOffset.UTC));
        var signed = storage.signUpload("users/a/videos/b/source", "video/mp4", null, java.time.Duration.ofMinutes(1));
        var capability = signed.url().substring(signed.url().lastIndexOf('/') + 1);
        assertThatThrownBy(() -> storage.put(capability, "video/mp4", "*", null, new ByteArrayInputStream("four".getBytes())))
            .isInstanceOf(LocalMockVideoObjectStorage.UploadTooLargeException.class);
    }

    @Test
    void verifiesChecksumWhenTheReservationRequiresIt() throws Exception {
        var bytes = "video".getBytes();
        var checksum = HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        var storage = storage(Instant.parse("2026-09-21T12:00:00Z"));
        var signed = storage.signUpload("users/a/videos/b/source", "video/mp4", checksum, java.time.Duration.ofMinutes(1));
        var capability = signed.url().substring(signed.url().lastIndexOf('/') + 1);

        storage.put(capability, "video/mp4", "*", signed.headers().get("x-amz-checksum-sha256"), new ByteArrayInputStream(bytes));

        assertThat(storage.stat("users/a/videos/b/source").orElseThrow().checksumSha256()).isEqualTo(checksum);
        assertThat(Files.exists(directory.resolve("users/a/videos/b/source"))).isTrue();
    }

    private LocalMockVideoObjectStorage storage(Instant now) {
        return new LocalMockVideoObjectStorage(500_000_000, "http://localhost:3001", directory,
            Clock.fixed(now, ZoneOffset.UTC));
    }
}
