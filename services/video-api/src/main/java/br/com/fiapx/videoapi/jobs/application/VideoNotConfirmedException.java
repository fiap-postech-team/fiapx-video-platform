package br.com.fiapx.videoapi.jobs.application;

public final class VideoNotConfirmedException extends RuntimeException {
    public VideoNotConfirmedException() {
        super("Video is not available for processing");
    }
}
