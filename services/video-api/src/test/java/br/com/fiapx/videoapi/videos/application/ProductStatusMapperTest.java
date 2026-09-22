package br.com.fiapx.videoapi.videos.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import br.com.fiapx.videoapi.videos.domain.VideoStatus;
import org.junit.jupiter.api.Test;

class ProductStatusMapperTest {

    @Test
    void mapsUploadAndJobCombinationsToProductEnums() {
        assertThat(ProductStatusMapper.videoStatus(VideoStatus.PENDING, null))
            .isEqualTo(ProductVideoStatus.AWAITING_UPLOAD);
        assertThat(ProductStatusMapper.videoStatus(VideoStatus.UPLOADED, null))
            .isEqualTo(ProductVideoStatus.UPLOADED);
        assertThat(ProductStatusMapper.videoStatus(VideoStatus.UPLOADED, JobStatus.PENDING))
            .isEqualTo(ProductVideoStatus.PROCESSING);
        assertThat(ProductStatusMapper.videoStatus(VideoStatus.UPLOADED, JobStatus.PROCESSING))
            .isEqualTo(ProductVideoStatus.PROCESSING);
        assertThat(ProductStatusMapper.videoStatus(VideoStatus.UPLOADED, JobStatus.COMPLETED))
            .isEqualTo(ProductVideoStatus.AVAILABLE);
        assertThat(ProductStatusMapper.videoStatus(VideoStatus.UPLOADED, JobStatus.FAILED))
            .isEqualTo(ProductVideoStatus.FAILED);
        assertThat(ProductStatusMapper.videoStatus(VideoStatus.REJECTED, null))
            .isEqualTo(ProductVideoStatus.REJECTED);
        assertThat(ProductStatusMapper.videoStatus(VideoStatus.EXPIRED, null))
            .isEqualTo(ProductVideoStatus.EXPIRED);
        assertThat(ProductStatusMapper.processingStatus(JobStatus.PENDING))
            .isEqualTo(ProductProcessingStatus.QUEUED);
        assertThat(ProductStatusMapper.processingStatus(JobStatus.COMPLETED))
            .isEqualTo(ProductProcessingStatus.AVAILABLE);
    }
}
