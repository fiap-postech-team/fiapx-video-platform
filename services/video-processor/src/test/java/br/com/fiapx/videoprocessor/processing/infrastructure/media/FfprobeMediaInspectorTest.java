package br.com.fiapx.videoprocessor.processing.infrastructure.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiapx.videoprocessor.processing.domain.MediaMetadata;
import br.com.fiapx.videoprocessor.processing.domain.TerminalProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class FfprobeMediaInspectorTest {

    private static final Path MEDIA = Paths.get("/workspace/job-1/source");
    private static final MediaProperties PROPERTIES = new MediaProperties(
            "ffmpeg", "ffprobe", 1, Duration.ofMinutes(30), Duration.ofMinutes(5));

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsTheDurationAndTheVideoStreamFromTheProbeOutput() {
        FakeCommandRunner runner = FakeCommandRunner.succeedingWith(
                """
                {"streams":[{"codec_type":"audio"},{"codec_type":"video"}],"format":{"duration":"12.500"}}
                """);

        MediaMetadata metadata = inspector(runner).inspect(MEDIA);

        assertThat(metadata.hasVideoStream()).isTrue();
        assertThat(metadata.duration()).isEqualTo(Duration.ofMillis(12_500));
    }

    @Test
    void reportsAnAudioOnlyFileAsHavingNoVideoStream() {
        FakeCommandRunner runner = FakeCommandRunner.succeedingWith(
                """
                {"streams":[{"codec_type":"audio"}],"format":{"duration":"5.0"}}
                """);

        assertThat(inspector(runner).inspect(MEDIA).hasVideoStream()).isFalse();
    }

    @Test
    void acceptsAStreamThatDoesNotDeclareItsDuration() {
        FakeCommandRunner runner = FakeCommandRunner.succeedingWith(
                """
                {"streams":[{"codec_type":"video"}],"format":{"duration":"N/A"}}
                """);

        assertThat(inspector(runner).inspect(MEDIA).duration()).isEqualTo(Duration.ZERO);
    }

    @Test
    void passesTheMediaPathAsASingleArgumentAndNeverThroughAShell() {
        FakeCommandRunner runner = FakeCommandRunner.succeedingWith(
                """
                {"streams":[{"codec_type":"video"}],"format":{"duration":"1.0"}}
                """);

        inspector(runner).inspect(MEDIA);

        assertThat(runner.command()).first().isEqualTo("ffprobe");
        assertThat(runner.command()).last().isEqualTo(MEDIA.toString());
        assertThat(runner.command()).containsSequence("-print_format", "json");
        assertThat(runner.command()).noneMatch(argument -> argument.contains("&&") || argument.contains("|"));
        assertThat(runner.timeout()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void rejectsMediaFfprobeCannotRead() {
        assertThatThrownBy(() -> inspector(FakeCommandRunner.failingWith(1)).inspect(MEDIA))
                .isInstanceOf(TerminalProcessingException.class)
                .hasMessage("The submitted file could not be read as video");
    }

    @Test
    void rejectsMediaThatMakesFfprobeHang() {
        assertThatThrownBy(() -> inspector(FakeCommandRunner.timingOut()).inspect(MEDIA))
                .isInstanceOf(TerminalProcessingException.class)
                .hasMessage("The submitted file could not be read as video");
    }

    @Test
    void rejectsProbeOutputThatIsNotJson() {
        assertThatThrownBy(() -> inspector(FakeCommandRunner.succeedingWith("not json")).inspect(MEDIA))
                .isInstanceOf(TerminalProcessingException.class)
                .hasMessage("The submitted file could not be read as video");
    }

    @Test
    void rejectsAVideoLongerThanTheConfiguredLimit() {
        FakeCommandRunner runner = FakeCommandRunner.succeedingWith(
                """
                {"streams":[{"codec_type":"video"}],"format":{"duration":"3600.0"}}
                """);

        assertThatThrownBy(() -> inspector(runner).inspect(MEDIA))
                .isInstanceOf(TerminalProcessingException.class)
                .hasMessage("The submitted video is longer than the allowed 30 minutes");
    }

    private FfprobeMediaInspector inspector(FakeCommandRunner runner) {
        return new FfprobeMediaInspector(runner, PROPERTIES, objectMapper);
    }
}
