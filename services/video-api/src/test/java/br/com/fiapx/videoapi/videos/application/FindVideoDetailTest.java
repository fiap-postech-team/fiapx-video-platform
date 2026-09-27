package br.com.fiapx.videoapi.videos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import br.com.fiapx.videoapi.videos.application.port.out.VideoLibraryReader;
import br.com.fiapx.videoapi.videos.domain.VideoStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FindVideoDetailTest {

    @Test
    void hidesMalformedReferencesAsNotFound() {
        assertThatThrownBy(() -> new FindVideoDetail(empty()).execute(UUID.randomUUID(), "bad"))
            .isInstanceOf(VideoLibraryNotFoundException.class);
    }

    @Test
    void mapsASingleProcessingWithoutInventingStartTime() {
        var videoId = UUID.randomUUID();
        var requested = Instant.parse("2026-09-20T14:11:13Z");
        var reader = new VideoLibraryReader() {
            public Page findPage(UUID ownerId, int pageNumber, int pageSize, VideoLibraryCriteria criteria) {
                return new Page(List.of(), 0);
            }
            public Optional<DetailRow> findDetail(UUID ownerId, UUID id) {
                return Optional.of(new DetailRow(videoId, "aula.mp4", VideoStatus.UPLOADED, requested,
                    requested, UUID.randomUUID(), JobStatus.PENDING, requested, null, null));
            }
        };

        var detail = new FindVideoDetail(reader).execute(UUID.randomUUID(), VideoRef.encode(videoId));

        assertThat(detail.status()).isEqualTo(ProductVideoStatus.PROCESSING);
        assertThat(detail.processing().status()).isEqualTo(ProductProcessingStatus.QUEUED);
        assertThat(detail.processing().startedAt()).isNull();
        assertThat(detail.processing().requestedAt()).isEqualTo(requested);
    }

    private static VideoLibraryReader empty() {
        return new VideoLibraryReader() {
            public Page findPage(UUID ownerId, int pageNumber, int pageSize, VideoLibraryCriteria criteria) {
                return new Page(List.of(), 0);
            }
            public Optional<DetailRow> findDetail(UUID ownerId, UUID videoId) {
                return Optional.empty();
            }
        };
    }
}
