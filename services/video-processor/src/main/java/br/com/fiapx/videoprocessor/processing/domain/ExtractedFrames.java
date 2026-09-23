package br.com.fiapx.videoprocessor.processing.domain;

import java.nio.file.Path;
import java.util.Objects;

public record ExtractedFrames(Path directory, int count) {

    public ExtractedFrames {
        Objects.requireNonNull(directory, "directory is required");
        if (count < 0) {
            throw new IllegalArgumentException("count cannot be negative");
        }
    }

    public boolean isEmpty() {
        return count == 0;
    }
}
