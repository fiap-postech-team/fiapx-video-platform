package br.com.fiapx.videoprocessor.processing.application.port.out;

import java.util.UUID;

public interface WorkspaceFactory {

    Workspace create(UUID jobId);
}
