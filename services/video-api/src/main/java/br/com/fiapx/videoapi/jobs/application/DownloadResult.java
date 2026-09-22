package br.com.fiapx.videoapi.jobs.application;

import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import br.com.fiapx.videoapi.videos.application.port.out.VideoObjectStorage;
import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class DownloadResult {
    private final JobStore jobs;
    private final VideoStore videos;
    private final VideoObjectStorage storage;
    private final Duration ttl;
    private final String mockUrl;

    public DownloadResult(JobStore jobs, VideoStore videos, VideoObjectStorage storage,
                          Duration ttl, String mockUrl) {
        this.jobs = jobs; this.videos = videos; this.storage = storage;
        this.ttl = ttl; this.mockUrl = mockUrl;
    }

    public Result execute(UUID jobId, UUID ownerId) {
        var job = jobs.findOwned(jobId, ownerId).orElseThrow(JobResultNotFoundException::new);
        if (job.status() != JobStatus.COMPLETED) throw new ResultNotReadyException();
        if (job.resultKey() == null || job.resultKey().isBlank()) throw new ResultInconsistentException();
        var object = storage.stat(job.resultKey()).orElseThrow(ResultObjectMissingException::new);
        var filename = filename(job, ownerId);
        var expires = Instant.now().plus(ttl);
        var url = mockUrl == null || mockUrl.isBlank()
            ? storage.signDownload(job.resultKey(), filename, ttl).url() : mockUrl;
        return new Result(url, expires, filename, object.contentType(), object.sizeBytes());
    }

    private String filename(br.com.fiapx.videoapi.jobs.domain.Job job, UUID ownerId) {
        var base = job.videoId() == null ? basename(job.sourceKey())
            : videos.findByJobOwner(job.videoId(), ownerId).map(v -> v.originalFilename())
                .orElseGet(() -> basename(job.sourceKey()));
        base = base == null ? "resultado" : base.replaceAll("[^A-Za-z0-9._-]", "-");
        if (base.toLowerCase().endsWith(".zip")) base = base.substring(0, base.length() - 4);
        return (base.isBlank() ? "resultado" : base) + "-" + job.id() + ".zip";
    }

    private String basename(String key) {
        if (key == null || key.isBlank()) return "resultado";
        var slash = key.lastIndexOf('/');
        return slash < 0 ? key : key.substring(slash + 1);
    }

    public record Result(String downloadUrl, Instant expiresAt, String filename,
                         String contentType, long sizeBytes) { }
}
