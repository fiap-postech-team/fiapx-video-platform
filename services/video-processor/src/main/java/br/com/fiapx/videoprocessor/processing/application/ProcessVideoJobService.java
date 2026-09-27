package br.com.fiapx.videoprocessor.processing.application;

import br.com.fiapx.videoprocessor.processing.application.port.in.ProcessVideoJob;
import br.com.fiapx.videoprocessor.processing.application.port.out.FrameArchiver;
import br.com.fiapx.videoprocessor.processing.application.port.out.FrameExtractor;
import br.com.fiapx.videoprocessor.processing.application.port.out.JobEventPublisher;
import br.com.fiapx.videoprocessor.processing.application.port.out.MediaInspector;
import br.com.fiapx.videoprocessor.processing.application.port.out.VideoObjectStorage;
import br.com.fiapx.videoprocessor.processing.application.port.out.Workspace;
import br.com.fiapx.videoprocessor.processing.application.port.out.WorkspaceFactory;
import br.com.fiapx.videoprocessor.processing.domain.ExtractedFrames;
import br.com.fiapx.videoprocessor.processing.domain.JobEvent;
import br.com.fiapx.videoprocessor.processing.domain.MediaMetadata;
import br.com.fiapx.videoprocessor.processing.domain.ResultLocation;
import br.com.fiapx.videoprocessor.processing.domain.TerminalProcessingException;
import br.com.fiapx.videoprocessor.processing.domain.VideoJob;
import java.nio.file.Path;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns a requested job into a ZIP of frames stored under a deterministic key.
 *
 * <p>The broker delivers at least once, so the stored result is the deduplication record: a job
 * whose ZIP already exists is answered with a completion event instead of being processed again.
 *
 * <p>A {@link TerminalProcessingException} means retrying cannot help, so it is reported as a
 * terminal failure and the message is acknowledged. Any other exception escapes, which lets the
 * listener container retry and eventually dead-letter the message.
 */
public class ProcessVideoJobService implements ProcessVideoJob {

    private static final Logger log = LoggerFactory.getLogger(ProcessVideoJobService.class);

    private static final String SOURCE_FILE = "source";
    private static final String FRAMES_DIRECTORY = "frames";
    private static final String ARCHIVE_FILE = "frames.zip";

    private final VideoObjectStorage storage;
    private final MediaInspector inspector;
    private final FrameExtractor frameExtractor;
    private final FrameArchiver archiver;
    private final JobEventPublisher publisher;
    private final WorkspaceFactory workspaces;
    private final Clock clock;

    public ProcessVideoJobService(
            VideoObjectStorage storage,
            MediaInspector inspector,
            FrameExtractor frameExtractor,
            FrameArchiver archiver,
            JobEventPublisher publisher,
            WorkspaceFactory workspaces,
            Clock clock) {
        this.storage = storage;
        this.inspector = inspector;
        this.frameExtractor = frameExtractor;
        this.archiver = archiver;
        this.publisher = publisher;
        this.workspaces = workspaces;
        this.clock = clock;
    }

    @Override
    public void handle(VideoJob job) {
        ResultLocation resultLocation = job.resultLocation();
        if (storage.exists(resultLocation)) {
            log.info("job {} already has a stored result, republishing completion", job.jobId());
            publisher.publish(JobEvent.completed(job, resultLocation, clock.instant()));
            return;
        }

        publisher.publish(JobEvent.processing(job, clock.instant()));

        try (Workspace workspace = workspaces.create(job.jobId())) {
            Path media = storage.download(job.sourceKey(), workspace.file(SOURCE_FILE));

            MediaMetadata metadata = inspector.inspect(media);
            if (!metadata.hasVideoStream()) {
                throw new TerminalProcessingException("The submitted file does not contain a video stream");
            }

            ExtractedFrames frames = frameExtractor.extract(media, workspace.directory(FRAMES_DIRECTORY));
            if (frames.isEmpty()) {
                throw new TerminalProcessingException("No frame could be extracted from the submitted video");
            }

            Path archive = archiver.archive(frames, workspace.file(ARCHIVE_FILE));
            storage.upload(resultLocation, archive);

            log.info("job {} produced {} frames stored at {}", job.jobId(), frames.count(), resultLocation.key());
            publisher.publish(JobEvent.completed(job, resultLocation, clock.instant()));
        } catch (TerminalProcessingException e) {
            log.warn("job {} failed permanently: {}", job.jobId(), e.reason());
            publisher.publish(JobEvent.failed(
                    job.jobId(), job.correlationId(), e.reason(), true, clock.instant(),
                    job.recipient(), job.videoName()));
        }
    }
}
