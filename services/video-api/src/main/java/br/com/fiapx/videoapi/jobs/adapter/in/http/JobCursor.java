package br.com.fiapx.videoapi.jobs.adapter.in.http;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

record JobCursor(Instant createdAt, UUID id) {
    static JobCursor decode(String value) {
        try {
            var parts = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8).split("\\|", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid job cursor");
            }
            return new JobCursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid job cursor", exception);
        }
    }

    String encode() {
        var source = createdAt + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(source.getBytes(StandardCharsets.UTF_8));
    }
}
