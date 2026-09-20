package br.com.fiapx.videoapi.videos.adapter.configuration;

import br.com.fiapx.videoapi.videos.application.ExpireVideoUploads;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public final class VideoCleanupSchedule {
    private final ExpireVideoUploads cleanup;

    public VideoCleanupSchedule(ExpireVideoUploads cleanup) {
        this.cleanup = cleanup;
    }

    @Scheduled(fixedDelayString = "${app.video.cleanup-interval:PT5M}")
    public void clean() {
        cleanup.run();
    }
}
