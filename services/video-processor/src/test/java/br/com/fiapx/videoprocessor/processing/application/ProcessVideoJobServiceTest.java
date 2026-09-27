package br.com.fiapx.videoprocessor.processing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.fiapx.videoprocessor.processing.application.port.out.FrameArchiver;
import br.com.fiapx.videoprocessor.processing.application.port.out.FrameExtractor;
import br.com.fiapx.videoprocessor.processing.application.port.out.JobEventPublisher;
import br.com.fiapx.videoprocessor.processing.application.port.out.MediaInspector;
import br.com.fiapx.videoprocessor.processing.application.port.out.VideoObjectStorage;
import br.com.fiapx.videoprocessor.processing.application.port.out.Workspace;
import br.com.fiapx.videoprocessor.processing.application.port.out.WorkspaceFactory;
import br.com.fiapx.videoprocessor.processing.domain.ExtractedFrames;
import br.com.fiapx.videoprocessor.processing.domain.JobEvent;
import br.com.fiapx.videoprocessor.processing.domain.JobEventType;
import br.com.fiapx.videoprocessor.processing.domain.MediaMetadata;
import br.com.fiapx.videoprocessor.processing.domain.TerminalProcessingException;
import br.com.fiapx.videoprocessor.processing.domain.VideoJob;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessVideoJobServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-30T20:00:00Z");

    @Mock
    private VideoObjectStorage storage;

    @Mock
    private MediaInspector inspector;

    @Mock
    private FrameExtractor frameExtractor;

    @Mock
    private FrameArchiver archiver;

    @Mock
    private JobEventPublisher publisher;

    @Mock
    private WorkspaceFactory workspaces;

    @TempDir
    Path workspaceRoot;

    private RecordingWorkspace workspace;
    private ProcessVideoJobService service;
    private VideoJob job;

    @BeforeEach
    void setUp() {
        workspace = new RecordingWorkspace(workspaceRoot);
        service = new ProcessVideoJobService(
                storage,
                inspector,
                frameExtractor,
                archiver,
                publisher,
                workspaces,
                Clock.fixed(NOW, ZoneOffset.UTC));
        job = new VideoJob(UUID.randomUUID(), UUID.randomUUID(), "uploads/video.mp4");
    }

    @Test
    void announcesProcessingThenStoresTheArchiveAndAnnouncesCompletion() {
        Path archive = givenAReadableVideoProducing(12);

        service.handle(job);

        InOrder events = inOrder(publisher);
        events.verify(publisher).publish(eventOfType(JobEventType.PROCESSING));
        events.verify(publisher).publish(eventOfType(JobEventType.COMPLETED));

        verify(storage).upload(job.resultLocation(), archive);

        List<JobEvent> published = publishedEvents();
        JobEvent completed = published.get(1);
        assertThat(completed.resultKey()).isEqualTo("results/" + job.jobId() + "/frames.zip");
        assertThat(completed.occurredAt()).isEqualTo(NOW);
        assertThat(completed.correlationId()).isEqualTo(job.correlationId());
        assertThat(completed.eventId()).isNotEqualTo(published.get(0).eventId());
        assertThat(completed.recipient()).isNull();
        assertThat(completed.videoName()).isNull();
        assertThat(workspace.isClosed()).isTrue();
    }

    @Test
    void copiesRecipientAndVideoNameOntoCompletionAndFailure() {
        VideoJob named = new VideoJob(
                job.jobId(), job.userId(), job.sourceKey(), job.correlationId(), "person@example.test", "aula.mp4");
        givenAReadableVideoProducing(2);

        service.handle(named);

        JobEvent completed = lastPublishedEvent();
        assertThat(completed.type()).isEqualTo(JobEventType.COMPLETED);
        assertThat(completed.recipient()).isEqualTo("person@example.test");
        assertThat(completed.videoName()).isEqualTo("aula.mp4");

        givenADownloadedSource();
        when(inspector.inspect(any())).thenReturn(new MediaMetadata(Duration.ofSeconds(3), false));
        service.handle(named);

        JobEvent failure = lastPublishedEvent();
        assertThat(failure.type()).isEqualTo(JobEventType.FAILED);
        assertThat(failure.recipient()).isEqualTo("person@example.test");
        assertThat(failure.videoName()).isEqualTo("aula.mp4");
    }

    @Test
    void republishesCompletionWithoutReprocessingWhenTheArchiveAlreadyExists() {
        when(storage.exists(job.resultLocation())).thenReturn(true);

        service.handle(job);

        assertThat(publishedEvents()).singleElement().satisfies(event -> {
            assertThat(event.type()).isEqualTo(JobEventType.COMPLETED);
            assertThat(event.resultKey()).isEqualTo(job.resultLocation().key());
        });
        verify(storage, never()).download(any(), any());
        verifyNoInteractions(workspaces, inspector, frameExtractor, archiver);
    }

    @Test
    void reportsATerminalFailureWhenTheSourceCarriesNoVideoStream() {
        givenADownloadedSource();
        when(inspector.inspect(any())).thenReturn(new MediaMetadata(Duration.ofSeconds(3), false));

        service.handle(job);

        JobEvent failure = lastPublishedEvent();
        assertThat(failure.type()).isEqualTo(JobEventType.FAILED);
        assertThat(failure.terminal()).isTrue();
        assertThat(failure.reason()).isEqualTo("The submitted file does not contain a video stream");
        verify(storage, never()).upload(any(), any());
        assertThat(workspace.isClosed()).isTrue();
    }

    @Test
    void reportsATerminalFailureWhenNoFrameCouldBeExtracted() {
        givenADownloadedSource();
        when(inspector.inspect(any())).thenReturn(new MediaMetadata(Duration.ofSeconds(3), true));
        when(frameExtractor.extract(any(), any())).thenReturn(new ExtractedFrames(workspaceRoot, 0));

        service.handle(job);

        JobEvent failure = lastPublishedEvent();
        assertThat(failure.type()).isEqualTo(JobEventType.FAILED);
        assertThat(failure.terminal()).isTrue();
        assertThat(failure.reason()).isEqualTo("No frame could be extracted from the submitted video");
        verifyNoInteractions(archiver);
        verify(storage, never()).upload(any(), any());
    }

    @Test
    void publishesTheInspectorReasonWithoutLeakingInternalDetail() {
        givenADownloadedSource();
        when(inspector.inspect(any()))
                .thenThrow(new TerminalProcessingException(
                        "The submitted file could not be read as video",
                        new IllegalStateException("ffprobe exited with 1 for /tmp/job-1/source")));

        service.handle(job);

        JobEvent failure = lastPublishedEvent();
        assertThat(failure.reason())
                .isEqualTo("The submitted file could not be read as video")
                .doesNotContain("ffprobe", "/tmp");
        assertThat(workspace.isClosed()).isTrue();
    }

    @Test
    void letsATransientStorageFailureEscapeSoTheContainerCanRetry() {
        when(storage.exists(job.resultLocation())).thenReturn(false);
        when(workspaces.create(job.jobId())).thenReturn(workspace);
        when(storage.download(any(), any())).thenThrow(new IllegalStateException("connection reset"));

        assertThatThrownBy(() -> service.handle(job)).isInstanceOf(IllegalStateException.class);

        assertThat(publishedEvents()).singleElement().extracting(JobEvent::type).isEqualTo(JobEventType.PROCESSING);
        assertThat(workspace.isClosed()).isTrue();
    }

    private Path givenAReadableVideoProducing(int frameCount) {
        Path source = givenADownloadedSource();
        Path framesDirectory = workspace.directory("frames");
        Path archive = workspace.file("frames.zip");
        when(inspector.inspect(source)).thenReturn(new MediaMetadata(Duration.ofSeconds(frameCount), true));
        when(frameExtractor.extract(source, framesDirectory))
                .thenReturn(new ExtractedFrames(framesDirectory, frameCount));
        when(archiver.archive(any(), any())).thenReturn(archive);
        return archive;
    }

    private Path givenADownloadedSource() {
        Path source = workspace.file("source");
        when(storage.exists(job.resultLocation())).thenReturn(false);
        when(workspaces.create(job.jobId())).thenReturn(workspace);
        when(storage.download(job.sourceKey(), source)).thenReturn(source);
        return source;
    }

    private List<JobEvent> publishedEvents() {
        ArgumentCaptor<JobEvent> captor = ArgumentCaptor.forClass(JobEvent.class);
        verify(publisher, atLeastOnce()).publish(captor.capture());
        return captor.getAllValues();
    }

    private JobEvent lastPublishedEvent() {
        List<JobEvent> events = publishedEvents();
        return events.get(events.size() - 1);
    }

    private static JobEvent eventOfType(JobEventType type) {
        return argThat(event -> event != null && event.type() == type);
    }

    private static final class RecordingWorkspace implements Workspace {

        private final Path root;
        private boolean closed;

        private RecordingWorkspace(Path root) {
            this.root = root;
        }

        @Override
        public Path root() {
            return root;
        }

        @Override
        public Path file(String name) {
            return root.resolve(name);
        }

        @Override
        public Path directory(String name) {
            Path directory = root.resolve(name);
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return directory;
        }

        @Override
        public void close() {
            closed = true;
        }

        private boolean isClosed() {
            return closed;
        }
    }
}
