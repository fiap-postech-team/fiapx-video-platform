package br.com.fiapx.videoprocessor.processing.infrastructure.messaging.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import br.com.fiapx.videoprocessor.processing.application.port.in.ProcessVideoJob;
import br.com.fiapx.videoprocessor.processing.domain.VideoJob;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

@ExtendWith(MockitoExtension.class)
class JobRequestedListenerTest {

    @Mock
    private ProcessVideoJob processVideoJob;

    @Test
    void handsAValidRequestToTheUseCase() {
        JobRequestedListener listener = new JobRequestedListener(processVideoJob);
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        listener.onJobRequested(
                new JobRequestedMessage(UUID.randomUUID(), jobId, userId, "uploads/video.mp4", correlationId));

        ArgumentCaptor<VideoJob> captor = ArgumentCaptor.forClass(VideoJob.class);
        verify(processVideoJob).handle(captor.capture());
        assertThat(captor.getValue())
                .isEqualTo(new VideoJob(jobId, userId, "uploads/video.mp4", correlationId));
    }

    @Test
    void correlatesByJobIdWhenTheProducerOmitsTheCorrelationId() {
        JobRequestedListener listener = new JobRequestedListener(processVideoJob);
        UUID jobId = UUID.randomUUID();

        listener.onJobRequested(
                new JobRequestedMessage(UUID.randomUUID(), jobId, UUID.randomUUID(), "uploads/video.mp4", null));

        ArgumentCaptor<VideoJob> captor = ArgumentCaptor.forClass(VideoJob.class);
        verify(processVideoJob).handle(captor.capture());
        assertThat(captor.getValue().correlationId()).isEqualTo(jobId);
    }

    @Test
    void carriesRecipientAndVideoNameIntoTheJob() {
        JobRequestedListener listener = new JobRequestedListener(processVideoJob);
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        listener.onJobRequested(new JobRequestedMessage(
                UUID.randomUUID(), jobId, userId, "uploads/video.mp4", null, "person@example.test", "aula.mp4"));

        ArgumentCaptor<VideoJob> captor = ArgumentCaptor.forClass(VideoJob.class);
        verify(processVideoJob).handle(captor.capture());
        assertThat(captor.getValue().recipient()).isEqualTo("person@example.test");
        assertThat(captor.getValue().videoName()).isEqualTo("aula.mp4");
    }

    @Test
    void rejectsWithoutRetryWhenAContractRequiredFieldIsMissing() {
        JobRequestedListener listener = new JobRequestedListener(processVideoJob);
        JobRequestedMessage withoutSourceKey =
                new JobRequestedMessage(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, null);

        assertThatThrownBy(() -> listener.onJobRequested(withoutSourceKey))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(processVideoJob);
    }

    @Test
    void rejectsWithoutRetryWhenThePayloadIsEmpty() {
        JobRequestedListener listener = new JobRequestedListener(processVideoJob);

        assertThatThrownBy(() -> listener.onJobRequested(null))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(processVideoJob);
    }
}
