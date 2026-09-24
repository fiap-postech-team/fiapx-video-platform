package br.com.fiapx.videoprocessor.processing.application.port.out;

import br.com.fiapx.videoprocessor.processing.domain.MediaMetadata;
import java.nio.file.Path;

public interface MediaInspector {

    /**
     * Reads media metadata, rejecting anything that is not decodable video.
     *
     * @throws br.com.fiapx.videoprocessor.processing.domain.TerminalProcessingException when the
     *     file is not readable media
     */
    MediaMetadata inspect(Path media);
}
