package br.com.fiapx.videoprocessor.processing.application.port.out;

import br.com.fiapx.videoprocessor.processing.domain.ExtractedFrames;
import java.nio.file.Path;

public interface FrameArchiver {

    Path archive(ExtractedFrames frames, Path target);
}
