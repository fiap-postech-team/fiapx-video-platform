package br.com.fiapx.videoapi.foundation.openapi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalOpenApiResourceTest {

    @Test
    void packagesTheCanonicalOpenApiDocumentAsStaticResource() throws IOException, URISyntaxException {
        var canonical = Files.readString(canonicalOpenApiPath());
        var packaged = new ClassPathResource("static/openapi.yaml")
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(packaged).isEqualTo(canonical);
    }

    private Path canonicalOpenApiPath() throws URISyntaxException {
        var location = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
        while (location != null) {
            var candidate = location.resolve("contracts/openapi.yaml");
            if (Files.exists(candidate)) {
                return candidate;
            }
            location = location.getParent();
        }
        throw new IllegalStateException("Cannot locate contracts/openapi.yaml");
    }
}
