package br.com.fiapx.videoprocessor.processing.application.port.out;

import br.com.fiapx.videoprocessor.processing.domain.ResultLocation;
import java.nio.file.Path;

public interface VideoObjectStorage {

    boolean exists(ResultLocation location);

    /**
     * Downloads {@code sourceKey} into {@code target} and returns the written path.
     */
    Path download(String sourceKey, Path target);

    void upload(ResultLocation location, Path source);
}
