package br.com.fiapx.videoapi.videos.application;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

public final class VideoRef {
    private VideoRef() {
    }

    public static String encode(UUID id) {
        var bytes = ByteBuffer.allocate(16);
        bytes.putLong(id.getMostSignificantBits());
        bytes.putLong(id.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.array());
    }

    public static Optional<UUID> decode(String videoRef) {
        if (videoRef == null || videoRef.isBlank()) {
            return Optional.empty();
        }
        try {
            var decoded = Base64.getUrlDecoder().decode(videoRef);
            if (decoded.length != 16) {
                return Optional.empty();
            }
            var bytes = ByteBuffer.wrap(decoded);
            return Optional.of(new UUID(bytes.getLong(), bytes.getLong()));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
