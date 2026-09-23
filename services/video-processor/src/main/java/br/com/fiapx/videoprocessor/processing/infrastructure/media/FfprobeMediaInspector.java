package br.com.fiapx.videoprocessor.processing.infrastructure.media;

import br.com.fiapx.videoprocessor.processing.application.port.out.MediaInspector;
import br.com.fiapx.videoprocessor.processing.domain.MediaMetadata;
import br.com.fiapx.videoprocessor.processing.domain.TerminalProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Reads media metadata with FFprobe before any expensive work happens.
 *
 * <p>A file FFprobe cannot read, or one longer than the configured limit, will not become readable
 * or shorter on a retry, so both are reported as terminal failures.
 */
@Component
public class FfprobeMediaInspector implements MediaInspector {

    private static final Logger log = LoggerFactory.getLogger(FfprobeMediaInspector.class);

    private static final String VIDEO_CODEC_TYPE = "video";
    private static final String UNREADABLE_MEDIA = "The submitted file could not be read as video";

    private final CommandRunner commandRunner;
    private final MediaProperties properties;
    private final ObjectMapper objectMapper;

    public FfprobeMediaInspector(CommandRunner commandRunner, MediaProperties properties, ObjectMapper objectMapper) {
        this.commandRunner = commandRunner;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public MediaMetadata inspect(Path media) {
        CommandRunner.CommandResult result = commandRunner.run(command(media), properties.commandTimeout());
        if (!result.isSuccessful()) {
            log.warn("ffprobe rejected the media (exitCode={}, timedOut={})", result.exitCode(), result.timedOut());
            throw new TerminalProcessingException(UNREADABLE_MEDIA);
        }

        JsonNode probe = parse(result.output());
        MediaMetadata metadata = new MediaMetadata(durationOf(probe), hasVideoStream(probe));

        if (metadata.duration().compareTo(properties.maxDuration()) > 0) {
            throw new TerminalProcessingException(
                    "The submitted video is longer than the allowed " + properties.maxDuration().toMinutes()
                            + " minutes");
        }
        return metadata;
    }

    private List<String> command(Path media) {
        return List.of(
                properties.ffprobePath(),
                "-v",
                "error",
                "-print_format",
                "json",
                "-show_format",
                "-show_streams",
                media.toString());
    }

    private JsonNode parse(String output) {
        try {
            return objectMapper.readTree(output);
        } catch (Exception e) {
            log.warn("ffprobe returned output that is not valid JSON", e);
            throw new TerminalProcessingException(UNREADABLE_MEDIA, e);
        }
    }

    private static boolean hasVideoStream(JsonNode probe) {
        for (JsonNode stream : probe.path("streams")) {
            if (VIDEO_CODEC_TYPE.equals(stream.path("codec_type").asText())) {
                return true;
            }
        }
        return false;
    }

    private static Duration durationOf(JsonNode probe) {
        String duration = probe.path("format").path("duration").asText("");
        try {
            return Duration.ofMillis(Math.round(Double.parseDouble(duration) * 1_000));
        } catch (NumberFormatException e) {
            // Streams without a declared duration are still processable; the frame count decides.
            return Duration.ZERO;
        }
    }
}
