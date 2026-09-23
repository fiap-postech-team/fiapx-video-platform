package br.com.fiapx.videoprocessor.processing.infrastructure.media;

import br.com.fiapx.videoprocessor.processing.application.port.out.FrameExtractor;
import br.com.fiapx.videoprocessor.processing.domain.ExtractedFrames;
import br.com.fiapx.videoprocessor.processing.domain.TerminalProcessingException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Extracts one PNG per configured frame interval. The output file name pattern is zero padded so
 * that a plain lexicographic sort keeps the frames in playback order inside the archive.
 */
@Component
public class FfmpegFrameExtractor implements FrameExtractor {

    private static final Logger log = LoggerFactory.getLogger(FfmpegFrameExtractor.class);

    static final String FRAME_NAME_PATTERN = "frame-%06d.png";

    private final CommandRunner commandRunner;
    private final MediaProperties properties;

    public FfmpegFrameExtractor(CommandRunner commandRunner, MediaProperties properties) {
        this.commandRunner = commandRunner;
        this.properties = properties;
    }

    @Override
    public ExtractedFrames extract(Path media, Path targetDirectory) {
        CommandRunner.CommandResult result =
                commandRunner.run(command(media, targetDirectory), properties.commandTimeout());

        if (result.timedOut()) {
            throw new TerminalProcessingException("The submitted video took too long to process");
        }
        if (!result.isSuccessful()) {
            log.warn("ffmpeg failed with exit code {}", result.exitCode());
            throw new TerminalProcessingException("The submitted video could not be decoded");
        }

        int count = countFrames(targetDirectory);
        log.debug("ffmpeg produced {} frames", count);
        return new ExtractedFrames(targetDirectory, count);
    }

    private List<String> command(Path media, Path targetDirectory) {
        return List.of(
                properties.ffmpegPath(),
                "-nostdin",
                "-hide_banner",
                "-loglevel",
                "error",
                "-y",
                "-i",
                media.toString(),
                "-vf",
                "fps=" + properties.framesPerSecond(),
                "-f",
                "image2",
                targetDirectory.resolve(FRAME_NAME_PATTERN).toString());
    }

    private static int countFrames(Path directory) {
        try (Stream<Path> files = Files.list(directory)) {
            return (int) files.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            throw new UncheckedIOException("could not count the extracted frames", e);
        }
    }
}
