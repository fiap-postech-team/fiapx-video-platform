package br.com.fiapx.videoprocessor.processing.infrastructure.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiapx.videoprocessor.processing.domain.ExtractedFrames;
import br.com.fiapx.videoprocessor.processing.domain.TerminalProcessingException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FfmpegFrameExtractorTest {

    private static final Path MEDIA = Paths.get("/workspace/job-1/source");
    private static final MediaProperties PROPERTIES = new MediaProperties(
            "ffmpeg", "ffprobe", 2, Duration.ofMinutes(30), Duration.ofMinutes(5));

    @TempDir
    Path frames;

    @Test
    void countsTheFramesTheToolWroteIntoTheTargetDirectory() {
        FakeCommandRunner runner = FakeCommandRunner.behavingAs(command -> writeFrames(3));

        ExtractedFrames extracted = new FfmpegFrameExtractor(runner, PROPERTIES).extract(MEDIA, frames);

        assertThat(extracted.directory()).isEqualTo(frames);
        assertThat(extracted.count()).isEqualTo(3);
        assertThat(extracted.isEmpty()).isFalse();
    }

    @Test
    void reportsAnEmptyResultWhenTheToolWroteNoFrame() {
        FakeCommandRunner runner = FakeCommandRunner.succeedingWith("");

        ExtractedFrames extracted = new FfmpegFrameExtractor(runner, PROPERTIES).extract(MEDIA, frames);

        assertThat(extracted.isEmpty()).isTrue();
    }

    @Test
    void buildsTheArgumentListWithTheConfiguredFrameRateAndAPaddedOutputPattern() {
        FakeCommandRunner runner = FakeCommandRunner.succeedingWith("");

        new FfmpegFrameExtractor(runner, PROPERTIES).extract(MEDIA, frames);

        assertThat(runner.command()).first().isEqualTo("ffmpeg");
        assertThat(runner.command()).contains("-nostdin");
        assertThat(runner.argumentAfter("-i")).isEqualTo(MEDIA.toString());
        assertThat(runner.argumentAfter("-vf")).isEqualTo("fps=2");
        assertThat(runner.command()).last().isEqualTo(frames.resolve("frame-%06d.png").toString());
        assertThat(runner.timeout()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void rejectsAVideoFfmpegCouldNotDecode() {
        FfmpegFrameExtractor extractor = new FfmpegFrameExtractor(FakeCommandRunner.failingWith(1), PROPERTIES);

        assertThatThrownBy(() -> extractor.extract(MEDIA, frames))
                .isInstanceOf(TerminalProcessingException.class)
                .hasMessage("The submitted video could not be decoded");
    }

    @Test
    void rejectsAVideoThatExceededTheCommandTimeout() {
        FfmpegFrameExtractor extractor = new FfmpegFrameExtractor(FakeCommandRunner.timingOut(), PROPERTIES);

        assertThatThrownBy(() -> extractor.extract(MEDIA, frames))
                .isInstanceOf(TerminalProcessingException.class)
                .hasMessage("The submitted video took too long to process");
    }

    private CommandRunner.CommandResult writeFrames(int count) {
        try {
            for (int index = 1; index <= count; index++) {
                Files.writeString(frames.resolve("frame-%06d.png".formatted(index)), "frame " + index);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return CommandRunner.CommandResult.finished(0, "");
    }
}
