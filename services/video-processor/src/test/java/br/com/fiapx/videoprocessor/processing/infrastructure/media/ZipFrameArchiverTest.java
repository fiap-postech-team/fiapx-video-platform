package br.com.fiapx.videoprocessor.processing.infrastructure.media;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiapx.videoprocessor.processing.domain.ExtractedFrames;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ZipFrameArchiverTest {

    @TempDir
    Path workspace;

    @Test
    void packsEveryFrameAsAFlatEntrySortedByName() throws IOException {
        Path frames = Files.createDirectory(workspace.resolve("frames"));
        Files.writeString(frames.resolve("frame-000002.png"), "second");
        Files.writeString(frames.resolve("frame-000001.png"), "first");
        Files.writeString(frames.resolve("frame-000003.png"), "third");
        Path target = workspace.resolve("frames.zip");

        Path archive = new ZipFrameArchiver().archive(new ExtractedFrames(frames, 3), target);

        assertThat(archive).isEqualTo(target).exists();
        assertThat(entryNamesOf(archive))
                .containsExactly("frame-000001.png", "frame-000002.png", "frame-000003.png");
    }

    @Test
    void keepsTheContentOfEachFrame() throws IOException {
        Path frames = Files.createDirectory(workspace.resolve("frames"));
        Files.writeString(frames.resolve("frame-000001.png"), "first frame bytes");

        Path archive = new ZipFrameArchiver().archive(new ExtractedFrames(frames, 1), workspace.resolve("frames.zip"));

        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            assertThat(zip.getNextEntry()).isNotNull();
            assertThat(new String(zip.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("first frame bytes");
        }
    }

    @Test
    void writesAnEmptyArchiveWhenThereIsNothingToPack() throws IOException {
        Path frames = Files.createDirectory(workspace.resolve("frames"));

        Path archive = new ZipFrameArchiver().archive(new ExtractedFrames(frames, 0), workspace.resolve("frames.zip"));

        assertThat(entryNamesOf(archive)).isEmpty();
    }

    private static List<String> entryNamesOf(Path archive) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }
}
