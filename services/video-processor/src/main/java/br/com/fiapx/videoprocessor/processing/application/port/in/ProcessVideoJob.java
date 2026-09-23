package br.com.fiapx.videoprocessor.processing.application.port.in;

import br.com.fiapx.videoprocessor.processing.domain.VideoJob;

public interface ProcessVideoJob {

    void handle(VideoJob job);
}
