package br.com.fiapx.videoprocessor.processing.domain;

import java.time.Duration;
import java.util.Objects;

public record MediaMetadata(Duration duration, boolean hasVideoStream) {

    public MediaMetadata {
        Objects.requireNonNull(duration, "duration is required");
    }
}
