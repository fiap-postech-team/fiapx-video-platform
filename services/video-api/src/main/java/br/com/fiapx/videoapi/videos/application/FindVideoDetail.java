package br.com.fiapx.videoapi.videos.application;

import br.com.fiapx.videoapi.videos.application.port.out.VideoLibraryReader;
import java.util.UUID;

public final class FindVideoDetail {
    private final VideoLibraryReader reader;

    public FindVideoDetail(VideoLibraryReader reader) {
        this.reader = reader;
    }

    public VideoLibraryDetail execute(UUID ownerId, String videoRef) {
        var videoId = VideoRef.decode(videoRef).orElseThrow(VideoLibraryNotFoundException::new);
        var row = reader.findDetail(ownerId, videoId).orElseThrow(VideoLibraryNotFoundException::new);
        return new VideoLibraryDetail(
            VideoRef.encode(row.videoId()),
            row.originalFilename(),
            ProductStatusMapper.videoStatus(row.uploadStatus(), row.jobStatus()),
            row.submittedAt(),
            row.uploadedAt(),
            processing(row)
        );
    }

    private static VideoLibraryDetail.ProcessingView processing(VideoLibraryReader.DetailRow row) {
        if (row.jobId() == null || row.jobStatus() == null) {
            return null;
        }
        return new VideoLibraryDetail.ProcessingView(
            row.jobId(),
            ProductStatusMapper.processingStatus(row.jobStatus()),
            row.jobCreatedAt(),
            row.startedAt(),
            row.finishedAt()
        );
    }
}
