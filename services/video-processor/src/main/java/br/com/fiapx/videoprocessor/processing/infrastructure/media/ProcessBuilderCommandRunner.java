package br.com.fiapx.videoprocessor.processing.infrastructure.media;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class ProcessBuilderCommandRunner implements CommandRunner {

    private static final Logger log = LoggerFactory.getLogger(ProcessBuilderCommandRunner.class);

    /**
     * Both tools can be chatty and the output only feeds diagnostics, so it is capped to keep a long
     * FFmpeg run from filling memory.
     */
    private static final int MAX_CAPTURED_CHARACTERS = 8_192;

    private static final Duration DRAIN_GRACE = Duration.ofSeconds(2);

    @Override
    public CommandResult run(List<String> command, Duration timeout) {
        Process process = start(command);
        AtomicReference<String> output = new AtomicReference<>("");

        // The output has to be drained on another thread: reading until EOF on this one would block
        // past the timeout whenever the tool hangs.
        Thread drain = Thread.ofVirtual().start(() -> output.set(read(process.getInputStream())));

        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                drain.join(DRAIN_GRACE);
                log.warn("command {} exceeded {} and was terminated", command.get(0), timeout);
                return CommandResult.timedOut(output.get());
            }
            drain.join(DRAIN_GRACE);
            return CommandResult.finished(process.exitValue(), output.get());
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new MediaCommandException("interrupted while running " + command.get(0), e);
        }
    }

    private Process start(List<String> command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            // Give the tool an immediate EOF on stdin so it can never wait for input.
            process.getOutputStream().close();
            return process;
        } catch (IOException e) {
            throw new MediaCommandException("could not start " + command.get(0), e);
        }
    }

    private String read(InputStream stream) {
        StringBuilder captured = new StringBuilder();
        byte[] buffer = new byte[4_096];
        try (stream) {
            int length;
            while ((length = stream.read(buffer)) != -1) {
                if (captured.length() < MAX_CAPTURED_CHARACTERS) {
                    captured.append(new String(buffer, 0, length, StandardCharsets.UTF_8));
                }
            }
        } catch (IOException e) {
            log.debug("stopped reading command output early", e);
        }
        return captured.toString();
    }
}
