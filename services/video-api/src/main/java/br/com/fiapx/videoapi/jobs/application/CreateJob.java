package br.com.fiapx.videoapi.jobs.application;

import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.application.port.out.JobCreationIdempotencyStore;
import br.com.fiapx.videoapi.jobs.domain.Job;
import br.com.fiapx.videoapi.outbox.application.port.out.OutboxStore;
import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

public final class CreateJob {
    private final JobStore jobs;
    private final OutboxStore outbox;
    private final VideoStore videos;
    private final JobCreationIdempotencyStore idempotency;
    private final Clock clock;

    public CreateJob(JobStore jobs, OutboxStore outbox, VideoStore videos, Clock clock) {
        this(jobs, outbox, videos, noIdempotency(), clock);
    }

    public CreateJob(JobStore jobs, OutboxStore outbox, VideoStore videos,
                     JobCreationIdempotencyStore idempotency, Clock clock) {
        this.jobs = jobs;
        this.outbox = outbox;
        this.videos = videos;
        this.idempotency = idempotency;
        this.clock = clock;
    }

    public Job execute(UUID userId, String sourceKey) {
        return execute(userId, sourceKey, null);
    }

    public Job execute(UUID userId, String sourceKey, String idempotencyKey) {
        var fingerprint = fingerprint(sourceKey);
        var existing = existingJob(userId, idempotencyKey, fingerprint);
        if (existing != null) {
            return existing;
        }
        var video = videos.findOwned(userId, sourceKey).orElseThrow(VideoNotFoundException::new);
        if (!video.isConfirmed()) {
            throw new VideoNotConfirmedException();
        }
        videos.lock(video.id()).orElseThrow(VideoNotFoundException::new);
        if (jobs.findVisibleByVideoId(video.id()).isPresent()) {
            throw new ProcessingAlreadyExistsException();
        }
        var job = jobs.save(new Job(UUID.randomUUID(), userId, video.id(), sourceKey, clock.instant()));
        if (idempotencyKey != null) {
            idempotency.record(userId, idempotencyKey, fingerprint, job.id());
        }
        outbox.append(UUID.randomUUID(), job.id(), userId, video.id(), sourceKey);
        return job;
    }

    private Job existingJob(UUID userId, String key, String fingerprint) {
        if (key == null) {
            return null;
        }
        idempotency.lock(userId, key);
        return idempotency.find(userId, key).map(recorded -> resolve(recorded, fingerprint, userId)).orElse(null);
    }

    private Job resolve(JobCreationIdempotencyStore.RecordedJobCreation recorded, String fingerprint, UUID userId) {
        if (!recorded.requestFingerprint().equals(fingerprint)) {
            throw new IdempotencyConflictException();
        }
        return jobs.findOwned(recorded.jobId(), userId).orElseThrow(IllegalStateException::new);
    }

    private String fingerprint(String sourceKey) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(sourceKey.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }

    private static JobCreationIdempotencyStore noIdempotency() {
        return new JobCreationIdempotencyStore() {
            public void lock(UUID userId, String key) {
            }

            public java.util.Optional<RecordedJobCreation> find(UUID userId, String key) {
                return java.util.Optional.empty();
            }

            public void record(UUID userId, String key, String fingerprint, UUID jobId) {
            }
        };
    }
}
