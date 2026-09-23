package br.com.fiapx.videoprocessor.processing.infrastructure.media;

import br.com.fiapx.videoprocessor.processing.application.port.out.FrameArchiver;
import br.com.fiapx.videoprocessor.processing.domain.ExtractedFrames;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Component;

/**
 * Packs the extracted frames into a single ZIP. Entries are flat and sorted by file name, so the
 * archive contents are the same for the same video regardless of the order the file system lists.
 */
@Component
public class ZipFrameArchiver implements FrameArchiver {

    @Override
    public Path archive(ExtractedFrames frames, Path target) {
        try (OutputStream file = Files.newOutputStream(target);
                ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(file))) {
            for (Path frame : sortedFrames(frames.directory())) {
                zip.putNextEntry(new ZipEntry(frame.getFileName().toString()));
                Files.copy(frame, zip);
                zip.closeEntry();
            }
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException("could not build the frame archive", e);
        }
    }

    private static List<Path> sortedFrames(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }
}
