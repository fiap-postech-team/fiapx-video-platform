package br.com.fiapx.videoprocessor.processing.infrastructure.storage;

/**
 * Storage access failure. It is treated as transient on purpose: the listener container retries and
 * only reports a terminal failure once the attempts are exhausted.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
