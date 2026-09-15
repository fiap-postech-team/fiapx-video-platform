package br.com.fiapx.videoapi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BootJarIT {

    private static final String APPLICATION_CLASS =
        "br.com.fiapx.videoapi.VideoApiApplication";

    @Test
    void packagesAnExecutableSpringBootJar() throws IOException {
        var jarPath = packagedJar();

        try (var jar = new JarFile(jarPath.toFile())) {
            assertThat(jar.getManifest().getMainAttributes().getValue("Main-Class"))
                .startsWith("org.springframework.boot.loader.");
            assertThat(jar.getManifest().getMainAttributes().getValue("Start-Class"))
                .isEqualTo(APPLICATION_CLASS);
            assertThat(jar.getEntry(
                "BOOT-INF/classes/br/com/fiapx/videoapi/VideoApiApplication.class"
            )).isNotNull();
        }
    }

    static Path packagedJar() throws IOException {
        try (var files = Files.list(Path.of("target"))) {
            return files
                .filter(path -> path.getFileName().toString().startsWith("video-api-"))
                .filter(path -> path.getFileName().toString().endsWith(".jar"))
                .findFirst()
                .orElseThrow();
        }
    }
}
