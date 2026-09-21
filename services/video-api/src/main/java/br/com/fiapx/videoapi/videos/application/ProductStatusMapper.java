package br.com.fiapx.videoapi.videos.application;

import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import br.com.fiapx.videoapi.videos.domain.VideoStatus;

public final class ProductStatusMapper {
    private ProductStatusMapper() {
    }

    public static ProductVideoStatus videoStatus(VideoStatus uploadStatus, JobStatus jobStatus) {
        if (jobStatus == JobStatus.COMPLETED) {
            return ProductVideoStatus.AVAILABLE;
        }
        if (jobStatus == JobStatus.FAILED) {
            return ProductVideoStatus.FAILED;
        }
        if (jobStatus == JobStatus.PENDING || jobStatus == JobStatus.PROCESSING) {
            return ProductVideoStatus.PROCESSING;
        }
        return switch (uploadStatus) {
            case PENDING -> ProductVideoStatus.AWAITING_UPLOAD;
            case REJECTED -> ProductVideoStatus.REJECTED;
            case EXPIRED -> ProductVideoStatus.EXPIRED;
            case UPLOADED, DELETED -> ProductVideoStatus.UPLOADED;
        };
    }

    public static ProductProcessingStatus processingStatus(JobStatus jobStatus) {
        return switch (jobStatus) {
            case PENDING -> ProductProcessingStatus.QUEUED;
            case PROCESSING -> ProductProcessingStatus.PROCESSING;
            case COMPLETED -> ProductProcessingStatus.AVAILABLE;
            case FAILED -> ProductProcessingStatus.FAILED;
        };
    }
}
