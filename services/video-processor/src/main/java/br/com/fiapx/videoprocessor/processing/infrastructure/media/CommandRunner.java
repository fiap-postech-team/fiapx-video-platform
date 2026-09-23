package br.com.fiapx.videoprocessor.processing.infrastructure.media;

import java.time.Duration;
import java.util.List;

/**
 * Runs an external media tool. The command is always a list of arguments, never a shell string, so
 * values taken from a message can never be interpreted as shell syntax.
 */
public interface CommandRunner {

    CommandResult run(List<String> command, Duration timeout);

    record CommandResult(int exitCode, String output, boolean timedOut) {

        public boolean isSuccessful() {
            return !timedOut && exitCode == 0;
        }

        public static CommandResult finished(int exitCode, String output) {
            return new CommandResult(exitCode, output, false);
        }

        public static CommandResult timedOut(String output) {
            return new CommandResult(-1, output, true);
        }
    }
}
