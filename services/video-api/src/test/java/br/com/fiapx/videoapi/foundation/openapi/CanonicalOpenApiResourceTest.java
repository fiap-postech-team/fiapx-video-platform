package br.com.fiapx.videoapi.foundation.openapi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalOpenApiResourceTest {

    @Test
    void packagesTheCanonicalOpenApiDocumentAsStaticResource() throws IOException {
        var canonical = Files.readString(Path.of("../../contracts/openapi.yaml"));
        var packaged = new ClassPathResource("static/openapi.yaml")
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(packaged).isEqualTo(canonical);
    }
}
