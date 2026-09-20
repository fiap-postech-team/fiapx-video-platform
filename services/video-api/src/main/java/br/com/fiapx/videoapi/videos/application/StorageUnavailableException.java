package br.com.fiapx.videoapi.videos.application;

public final class StorageUnavailableException extends RuntimeException {
    public StorageUnavailableException(Throwable cause) {
        super("Video storage unavailable", cause);
    }
}
