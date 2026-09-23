package br.com.fiapx.videoprocessor.processing.infrastructure.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiapx.videoprocessor.processing.application.port.out.Workspace;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TempDirectoryWorkspaceFactoryTest {

    @TempDir
    Path root;

    @Test
    void givesEachJobAnIsolatedDirectory() {
        TempDirectoryWorkspaceFactory factory = new TempDirectoryWorkspaceFactory(new WorkspaceProperties(root));

        try (Workspace first = factory.create(UUID.randomUUID());
                Workspace second = factory.create(UUID.randomUUID())) {
            assertThat(first.root()).isNotEqualTo(second.root()).exists();
            assertThat(second.root()).exists();
        }
    }

    @Test
    void removesEveryTemporaryArtifactOnClose() throws IOException {
        TempDirectoryWorkspaceFactory factory = new TempDirectoryWorkspaceFactory(new WorkspaceProperties(root));
        Path workspaceRoot;

        try (Workspace workspace = factory.create(UUID.randomUUID())) {
            workspaceRoot = workspace.root();
            Files.writeString(workspace.file("source"), "video bytes");
            Files.writeString(workspace.directory("frames").resolve("frame-000001.png"), "frame bytes");
        }

        assertThat(workspaceRoot).doesNotExist();
    }

    @Test
    void createsTheConfiguredRootWhenItIsMissing() {
        Path missingRoot = root.resolve("nested").resolve("workspaces");
        TempDirectoryWorkspaceFactory factory = new TempDirectoryWorkspaceFactory(new WorkspaceProperties(missingRoot));

        try (Workspace workspace = factory.create(UUID.randomUUID())) {
            assertThat(workspace.root()).exists().hasParent(missingRoot);
        }
    }

    @Test
    void fallsBackToTheSystemTemporaryDirectoryWhenNoRootIsConfigured() {
        assertThat(new WorkspaceProperties(null).root())
                .isEqualTo(Path.of(System.getProperty("java.io.tmpdir")));
    }
}
