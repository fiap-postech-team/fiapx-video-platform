package br.com.fiapx.videoprocessor.processing.infrastructure.media;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param framesPerSecond how many frames FFmpeg extracts per second of video
 * @param maxDuration longest video accepted; longer sources are rejected before FFmpeg runs
 * @param commandTimeout wall-clock limit applied to each FFprobe and FFmpeg invocation
 */
@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(
        String ffmpegPath, String ffprobePath, int framesPerSecond, Duration maxDuration, Duration commandTimeout) {

    public MediaProperties {
        if (framesPerSecond <= 0) {
            throw new IllegalArgumentException("app.media.frames-per-second must be greater than zero");
        }
    }
}
