package br.com.fiapx.videoapi.videos.adapter.in.http;

import br.com.fiapx.videoapi.identity.domain.AuthenticatedIdentity;
import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.domain.Job;
import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import br.com.fiapx.videoapi.videos.domain.Video;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Owner-scoped product reads. Storage keys and internal owner IDs are not returned. */
@RestController
@RequestMapping("/v1/videos")
public final class VideoLibraryController {
    private static final int PAGE_SIZE = 20;
    private final VideoStore videos;
    private final JobStore jobs;

    public VideoLibraryController(VideoStore videos, JobStore jobs) { this.videos = videos; this.jobs = jobs; }

    @GetMapping
    VideoPage list(@RequestParam(defaultValue = "1") int page,
                   @AuthenticationPrincipal AuthenticatedIdentity identity) {
        if (page < 1) throw new IllegalArgumentException("Invalid page");
        var total = videos.countOwned(identity.userId());
        var rows = videos.findOwnedPage(identity.userId(), Math.multiplyExact(page - 1, PAGE_SIZE), PAGE_SIZE)
            .stream().map(video -> Item.from(video, jobs.findForVideo(identity.userId(), video.id()).orElse(null))).toList();
        return new VideoPage(rows, page, PAGE_SIZE, total, (int) Math.ceil((double) total / PAGE_SIZE));
    }

    @GetMapping("/{videoRef}")
    Detail detail(@PathVariable String videoRef, @AuthenticationPrincipal AuthenticatedIdentity identity) {
        var videoId = decode(videoRef);
        var video = videos.findOwnedById(identity.userId(), videoId).orElseThrow(br.com.fiapx.videoapi.videos.application.VideoUploadNotFoundException::new);
        return Detail.from(video, jobs.findForVideo(identity.userId(), video.id()).orElse(null));
    }

    private static UUID decode(String value) {
        try {
            var bytes = Base64.getUrlDecoder().decode(value);
            if (bytes.length != 16) throw new IllegalArgumentException();
            var buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        } catch (IllegalArgumentException exception) {
            throw new br.com.fiapx.videoapi.videos.application.VideoUploadNotFoundException();
        }
    }

    private static String ref(UUID id) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(ByteBuffer.allocate(16)
            .putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array());
    }

    record VideoPage(java.util.List<Item> items, int page, int pageSize, long totalItems, int totalPages) { }
    record Item(String videoRef, String originalFilename, String status, java.time.Instant submittedAt, java.time.Instant activityAt) {
        static Item from(Video video, Job job) {
            return new Item(ref(video.id()), video.originalFilename(), lifecycleStatus(video, job), video.createdAt(),
                job == null ? video.updatedAt() : job.createdAt());
        }
    }
    record Detail(String videoRef, String originalFilename, String status, java.time.Instant submittedAt,
                  java.time.Instant uploadedAt, Processing processing) {
        static Detail from(Video video, Job job) {
            return new Detail(ref(video.id()), video.originalFilename(), lifecycleStatus(video, job), video.createdAt(),
                video.uploadedAt(), job == null ? null : Processing.from(job));
        }
    }
    record Processing(String status, java.time.Instant requestedAt) {
        static Processing from(Job job) { return new Processing(job.status().name(), job.createdAt()); }
    }
    private static String lifecycleStatus(Video video, Job job) {
        if (job != null) return job.status().name();
        return video.status().name();
    }
}
