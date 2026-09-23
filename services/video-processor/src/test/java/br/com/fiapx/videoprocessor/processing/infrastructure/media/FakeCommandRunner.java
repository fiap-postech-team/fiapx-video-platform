package br.com.fiapx.videoprocessor.processing.infrastructure.media;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Stands in for FFprobe and FFmpeg so the media adapters can be tested without the tools installed,
 * while still recording the exact argument list they would have received.
 */
final class FakeCommandRunner implements CommandRunner {

    private final Function<List<String>, CommandResult> behaviour;

    private List<String> command;
    private Duration timeout;

    private FakeCommandRunner(Function<List<String>, CommandResult> behaviour) {
        this.behaviour = behaviour;
    }

    static FakeCommandRunner succeedingWith(String output) {
        return new FakeCommandRunner(command -> CommandResult.finished(0, output));
    }

    static FakeCommandRunner failingWith(int exitCode) {
        return new FakeCommandRunner(command -> CommandResult.finished(exitCode, ""));
    }

    static FakeCommandRunner timingOut() {
        return new FakeCommandRunner(command -> CommandResult.timedOut(""));
    }

    static FakeCommandRunner behavingAs(Function<List<String>, CommandResult> behaviour) {
        return new FakeCommandRunner(behaviour);
    }

    @Override
    public CommandResult run(List<String> command, Duration timeout) {
        this.command = List.copyOf(command);
        this.timeout = timeout;
        return behaviour.apply(this.command);
    }

    List<String> command() {
        return command;
    }

    Duration timeout() {
        return timeout;
    }

    String argumentAfter(String flag) {
        return command.get(command.indexOf(flag) + 1);
    }
}
