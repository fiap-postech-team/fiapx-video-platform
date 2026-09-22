package br.com.fiapx.videoapi.videos.adapter.out.persistence;

import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import br.com.fiapx.videoapi.videos.application.port.out.VideoLibraryReader;
import br.com.fiapx.videoapi.videos.domain.VideoStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public final class JpaVideoLibraryReader implements VideoLibraryReader {
    private final VideoLibraryQueryRepository queries;

    public JpaVideoLibraryReader(VideoLibraryQueryRepository queries) {
        this.queries = queries;
    }

    public Page findPage(UUID ownerId, int pageNumber, int pageSize) {
        var result = queries.findPage(ownerId, PageRequest.of(pageNumber - 1, pageSize));
        return new Page(result.getContent().stream().map(this::row).toList(), result.getTotalElements());
    }

    public Optional<DetailRow> findDetail(UUID ownerId, UUID videoId) {
        return queries.findDetail(ownerId, videoId).map(this::detail);
    }

    private Row row(VideoLibraryQueryRepository.VideoLibraryQuery query) {
        return new Row(
            query.getVideoId(),
            query.getOriginalFilename(),
            VideoStatus.valueOf(query.getUploadStatus()),
            query.getSubmittedAt(),
            query.getJobId(),
            jobStatus(query.getJobStatus()),
            query.getJobCreatedAt(),
            query.getJobUpdatedAt()
        );
    }

    private DetailRow detail(VideoLibraryQueryRepository.VideoLibraryQuery query) {
        Instant startedAt = null;
        Instant finishedAt = null;
        if (query.getJobId() != null) {
            for (var event : queries.findHistory(query.getJobId())) {
                if (startedAt == null && "PROCESSING".equals(event.getStatus())) {
                    startedAt = event.getOccurredAt();
                }
                if (finishedAt == null && ("COMPLETED".equals(event.getStatus()) || "FAILED".equals(event.getStatus()))) {
                    finishedAt = event.getOccurredAt();
                }
            }
        }
        return new DetailRow(
            query.getVideoId(),
            query.getOriginalFilename(),
            VideoStatus.valueOf(query.getUploadStatus()),
            query.getSubmittedAt(),
            query.getUploadedAt(),
            query.getJobId(),
            jobStatus(query.getJobStatus()),
            query.getJobCreatedAt(),
            startedAt,
            finishedAt
        );
    }

    private static JobStatus jobStatus(String status) {
        return status == null ? null : JobStatus.valueOf(status);
    }
}
