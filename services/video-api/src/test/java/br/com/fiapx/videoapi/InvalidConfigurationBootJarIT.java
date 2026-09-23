package br.com.fiapx.videoapi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static br.com.fiapx.videoapi.BootJarIT.packagedJar;

class InvalidConfigurationBootJarIT {

    @Test
    void rejectsMissingDatabaseConfiguration() throws Exception {
        var result = runJar(
            "--spring.datasource.url=",
            "--spring.datasource.username=",
            "--spring.datasource.password="
        );

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("spring.datasource.url");
    }

    @Test
    void rejectsLocalDefaultsOutsideLocalProfileWithoutLeakingThem() throws Exception {
        var localUrl = "jdbc:postgresql://localhost:5432/fiapx";
        var result = runJar(
            "--spring.datasource.url=" + localUrl,
            "--spring.datasource.username=fiapx",
            "--spring.datasource.password=fiapx"
        );

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("spring.datasource.url").doesNotContain(localUrl);
    }

    private ProcessResult runJar(String... datasourceArguments) throws Exception {
        var command = new java.util.ArrayList<String>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-jar");
        command.add(packagedJar().toString());
        command.add("--spring.main.web-application-type=none");
        command.add("--spring.flyway.enabled=false");
        command.add("--spring.jpa.hibernate.ddl-auto=none");
        command.add("--management.health.db.enabled=false");
        command.addAll(java.util.List.of(datasourceArguments));

        var process = new ProcessBuilder(command).redirectErrorStream(true).start();
        if (!process.waitFor(20, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("Invalid configuration process did not terminate");
        }
        var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return new ProcessResult(process.exitValue(), output);
    }

    private record ProcessResult(int exitCode, String output) {
    }
}
