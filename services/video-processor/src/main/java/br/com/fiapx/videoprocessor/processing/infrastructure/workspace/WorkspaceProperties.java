package br.com.fiapx.videoprocessor.processing.infrastructure.workspace;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param root directory holding the per-job scratch areas; defaults to the system temp directory
 */
@ConfigurationProperties(prefix = "app.workspace")
public record WorkspaceProperties(Path root) {

    public WorkspaceProperties {
        root = root == null ? Paths.get(System.getProperty("java.io.tmpdir")) : root;
    }
}
