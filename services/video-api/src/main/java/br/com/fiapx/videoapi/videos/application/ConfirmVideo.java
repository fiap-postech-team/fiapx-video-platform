package br.com.fiapx.videoapi.videos.application;

import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import br.com.fiapx.videoapi.videos.domain.Video;
import java.time.Clock;

public final class ConfirmVideo {
    private final VideoStore videos;
    private final Clock clock;

    public ConfirmVideo(VideoStore videos, Clock clock) {
        this.videos = videos;
        this.clock = clock;
    }

    public Video execute(Video video) {
        return videos.save(video.confirm(clock.instant()));
    }
}
