package br.com.fiapx.videoprocessor.processing.infrastructure.media;

/**
 * The media tool could not be launched at all, which points at the environment rather than at the
 * video. It stays transient so the container retries before the job is declared failed.
 */
public class MediaCommandException extends RuntimeException {

    public MediaCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
