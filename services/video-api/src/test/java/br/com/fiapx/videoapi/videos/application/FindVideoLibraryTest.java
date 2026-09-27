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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class FindVideoLibraryTest {

    @Test
    void rejectsPagesBelowOne() {
        assertThatThrownBy(() -> new FindVideoLibrary(emptyReader()).execute(UUID.randomUUID(), 0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void usesJobActivityWhenPresentAndSubmitTimeOtherwise() {
        var withJob = UUID.randomUUID();
        var withoutJob = UUID.randomUUID();
        var submitted = Instant.parse("2026-09-20T10:00:00Z");
        var activity = Instant.parse("2026-09-20T11:00:00Z");
        var reader = new VideoLibraryReader() {
            public Page findPage(UUID ownerId, int pageNumber, int pageSize, VideoLibraryCriteria criteria) {
                return new Page(List.of(
                    new Row(withJob, "aula.mp4", VideoStatus.UPLOADED, submitted, UUID.randomUUID(), JobStatus.COMPLETED,
                        submitted, activity),
                    new Row(withoutJob, "rascunho.mp4", VideoStatus.PENDING, submitted, null, null, null, null)
                ), 2);
            }
            public Optional<DetailRow> findDetail(UUID ownerId, UUID videoId) {
                return Optional.empty();
            }
        };

        var page = new FindVideoLibrary(reader).execute(UUID.randomUUID(), 1);

        assertThat(page.pageSize()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.items().getFirst().status()).isEqualTo(ProductVideoStatus.AVAILABLE);
        assertThat(page.items().getFirst().activityAt()).isEqualTo(activity);
        assertThat(page.items().getLast().status()).isEqualTo(ProductVideoStatus.AWAITING_UPLOAD);
        assertThat(page.items().getLast().activityAt()).isEqualTo(submitted);
        assertThat(page.items().getFirst().videoRef()).isEqualTo(VideoRef.encode(withJob));
    }

    @Test
    void forwardsCriteriaToTheOwnerScopedReader() {
        var captured = new AtomicReference<VideoLibraryCriteria>();
        var reader = new VideoLibraryReader() {
            public Page findPage(UUID ownerId, int pageNumber, int pageSize, VideoLibraryCriteria criteria) {
                captured.set(criteria);
                return new Page(List.of(), 0);
            }
            public Optional<DetailRow> findDetail(UUID ownerId, UUID videoId) {
                return Optional.empty();
            }
        };
        var criteria = new VideoLibraryCriteria(
            "Black",
            VideoLibraryNameMatch.EXACT,
            VideoLibraryStatusFilter.PROCESSED
        );

        new FindVideoLibrary(reader).execute(UUID.randomUUID(), 1, criteria);

        assertThat(captured).hasValue(criteria);
    }

    @Test
    void reportsZeroPagesWhenTheOwnerHasNoVideos() {
        var page = new FindVideoLibrary(emptyReader()).execute(UUID.randomUUID(), 1);
        assertThat(page.items()).isEmpty();
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(0);
        assertThat(page.totalItems()).isZero();
    }

    private static VideoLibraryReader emptyReader() {
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
