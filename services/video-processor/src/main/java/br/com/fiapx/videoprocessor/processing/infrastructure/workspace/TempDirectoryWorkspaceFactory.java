package br.com.fiapx.videoprocessor.processing.infrastructure.workspace;

import br.com.fiapx.videoprocessor.processing.application.port.out.Workspace;
import br.com.fiapx.videoprocessor.processing.application.port.out.WorkspaceFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TempDirectoryWorkspaceFactory implements WorkspaceFactory {

    private static final Logger log = LoggerFactory.getLogger(TempDirectoryWorkspaceFactory.class);

    private final WorkspaceProperties properties;

    public TempDirectoryWorkspaceFactory(WorkspaceProperties properties) {
        this.properties = properties;
    }

    @Override
    public Workspace create(UUID jobId) {
        try {
            Files.createDirectories(properties.root());
            return new TempDirectoryWorkspace(Files.createTempDirectory(properties.root(), "job-" + jobId + "-"));
        } catch (IOException e) {
            throw new UncheckedIOException("could not create a workspace for job " + jobId, e);
        }
    }

    private record TempDirectoryWorkspace(Path root) implements Workspace {

        @Override
        public Path file(String name) {
            return root.resolve(name);
        }

        @Override
        public Path directory(String name) {
            Path directory = root.resolve(name);
            try {
                Files.createDirectories(directory);
                return directory;
            } catch (IOException e) {
                throw new UncheckedIOException("could not create the workspace directory " + name, e);
            }
        }

        @Override
        public void close() {
            // Cleanup must never mask the outcome of the job, so a failure here is only logged.
            try (Stream<Path> entries = Files.walk(root)) {
                entries.sorted(Comparator.reverseOrder()).forEach(this::deleteQuietly);
            } catch (IOException e) {
                log.warn("could not remove the workspace {}", root, e);
            }
        }

        private void deleteQuietly(Path path) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("could not remove {}", path, e);
            }
        }
    }
}
