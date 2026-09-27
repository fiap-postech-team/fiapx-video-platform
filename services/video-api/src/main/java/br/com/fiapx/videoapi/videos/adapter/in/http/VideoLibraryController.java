package br.com.fiapx.videoapi.videos.adapter.in.http;

import br.com.fiapx.videoapi.identity.domain.AuthenticatedIdentity;
import br.com.fiapx.videoapi.videos.application.FindVideoDetail;
import br.com.fiapx.videoapi.videos.application.FindVideoLibrary;
import br.com.fiapx.videoapi.videos.application.ProductProcessingStatus;
import br.com.fiapx.videoapi.videos.application.ProductVideoStatus;
import br.com.fiapx.videoapi.videos.application.VideoLibraryCriteria;
import br.com.fiapx.videoapi.videos.application.VideoLibraryDetail;
import br.com.fiapx.videoapi.videos.application.VideoLibraryNameMatch;
import br.com.fiapx.videoapi.videos.application.VideoLibraryPage;
import br.com.fiapx.videoapi.videos.application.VideoLibraryStatusFilter;
import br.com.fiapx.videoapi.videos.application.VideoLibrarySort;
import br.com.fiapx.videoapi.videos.application.VideoLibrarySortDirection;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/videos")
public class VideoLibraryController {
    private final FindVideoLibrary library;
    private final FindVideoDetail detail;

    public VideoLibraryController(FindVideoLibrary library, FindVideoDetail detail) {
        this.library = library;
        this.detail = detail;
    }

    @GetMapping
    LibraryPageResponse list(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(required = false) String name,
                             @RequestParam(defaultValue = "PREFIX") VideoLibraryNameMatch match,
                             @RequestParam(defaultValue = "ALL") VideoLibraryStatusFilter status,
                             @RequestParam(defaultValue = "UPDATED_AT") VideoLibrarySort sort,
                             @RequestParam(defaultValue = "DESC") VideoLibrarySortDirection direction,
                             @AuthenticationPrincipal AuthenticatedIdentity identity) {
        var criteria = new VideoLibraryCriteria(name, match, status, sort, direction);
        return LibraryPageResponse.from(library.execute(identity.userId(), page, criteria));
    }

    @GetMapping("/{videoRef}")
    LibraryDetailResponse get(@PathVariable String videoRef,
                              @AuthenticationPrincipal AuthenticatedIdentity identity) {
        return LibraryDetailResponse.from(detail.execute(identity.userId(), videoRef));
    }

    record LibraryPageResponse(List<LibraryItemResponse> items, int page, int pageSize,
                               long totalItems, int totalPages) {
        static LibraryPageResponse from(VideoLibraryPage result) {
            return new LibraryPageResponse(
                result.items().stream().map(LibraryItemResponse::from).toList(),
                result.page(), result.pageSize(), result.totalItems(), result.totalPages());
        }
    }

    record LibraryItemResponse(String videoRef, String originalFilename, ProductVideoStatus status,
                               UUID jobId,
                               Instant submittedAt, Instant activityAt) {
        static LibraryItemResponse from(VideoLibraryPage.VideoLibraryItem item) {
            return new LibraryItemResponse(item.videoRef(), item.originalFilename(), item.status(),
                item.jobId(), item.submittedAt(), item.activityAt());
        }
    }

    record LibraryDetailResponse(String videoRef, String originalFilename, ProductVideoStatus status,
                                 Instant submittedAt, Instant uploadedAt, ProcessingResponse processing) {
        static LibraryDetailResponse from(VideoLibraryDetail item) {
            return new LibraryDetailResponse(item.videoRef(), item.originalFilename(), item.status(),
                item.submittedAt(), item.uploadedAt(), ProcessingResponse.from(item.processing()));
        }
    }

    record ProcessingResponse(UUID jobId, ProductProcessingStatus status, Instant requestedAt,
                              Instant startedAt, Instant finishedAt) {
        static ProcessingResponse from(VideoLibraryDetail.ProcessingView processing) {
            return processing == null ? null
                : new ProcessingResponse(processing.jobId(), processing.status(), processing.requestedAt(),
                    processing.startedAt(), processing.finishedAt());
        }
    }
}
