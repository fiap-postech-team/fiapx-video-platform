package br.com.fiapx.videoprocessor.processing.application.port.out;

import java.nio.file.Path;

/**
 * Scratch area for a single job. Closing it removes every temporary artifact, so a worker restart
 * never leaves partial downloads or frames behind.
 */
public interface Workspace extends AutoCloseable {

    Path root();

    /**
     * Resolves a file path inside the workspace without creating it.
     */
    Path file(String name);

    /**
     * Resolves a directory inside the workspace, creating it when missing.
     */
    Path directory(String name);

    @Override
    void close();
}
