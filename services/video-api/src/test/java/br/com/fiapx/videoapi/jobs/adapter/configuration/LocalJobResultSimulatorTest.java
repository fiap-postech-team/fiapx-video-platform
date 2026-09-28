package br.com.fiapx.videoapi.jobs.adapter.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiapx.videoapi.identity.application.port.out.IdentityStore;
import br.com.fiapx.videoapi.identity.domain.IdentityUser;
import br.com.fiapx.videoapi.inbox.application.ProcessJobResult;
import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.application.port.out.UuidGenerator;
import br.com.fiapx.videoapi.jobs.domain.Job;
import br.com.fiapx.videoapi.jobs.domain.JobSourceKind;
import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import br.com.fiapx.videoapi.outbox.adapter.configuration.OutboxMessagingConfiguration;
import br.com.fiapx.videoapi.videos.application.port.out.VideoStore;
import br.com.fiapx.videoapi.videos.domain.Video;
import br.com.fiapx.videoapi.videos.domain.VideoStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class LocalJobResultSimulatorTest {
    private static final Instant NOW = Instant.parse("2026-09-28T12:00:00Z");

    @Mock JobStore jobs;
    @Mock ProcessJobResult processJobResult;
    @Mock UuidGenerator ids;
    @Mock IdentityStore identities;
    @Mock VideoStore videos;
    @Mock RabbitTemplate rabbit;
    @Mock TransactionTemplate transactions;
    @Mock IdentityUser user;

    private final ObjectMapper json = new ObjectMapper();
    private LocalJobResultSimulator simulator;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            invocation.getArgument(0, java.util.function.Consumer.class).accept(null);
            return null;
        }).when(transactions).executeWithoutResult(any());
        simulator = new LocalJobResultSimulator(
            jobs, processJobResult, Clock.fixed(NOW, ZoneOffset.UTC),
            new VideoLocalDemoProperties(JobStatus.COMPLETED), transactions, ids,
            identities, videos, rabbit, json);
    }

    @Test
    void doesNotNotifyWhenTheJobOnlyMovesToProcessing() {
        var job = job(JobStatus.PENDING);
        when(jobs.findAwaitingLocalDemo(25)).thenReturn(List.of(job));
        when(ids.next()).thenReturn(UUID.randomUUID());

        simulator.simulateResult();

        verify(rabbit, never()).convertAndSend(any(), any(), any(), any(MessagePostProcessor.class));
    }

    @Test
    void publishesACompletedNotificationWithTheOwnerAndVideoName() throws Exception {
        var job = job(JobStatus.PROCESSING);
        var eventId = UUID.randomUUID();
        when(jobs.findAwaitingLocalDemo(25)).thenReturn(List.of(job));
        when(ids.next()).thenReturn(eventId);
        when(user.email()).thenReturn("person@example.test");
        when(identities.findUser(job.userId())).thenReturn(Optional.of(user));
        when(videos.findOwnedById(job.userId(), job.videoId())).thenReturn(Optional.of(video(job)));

        simulator.simulateResult();

        ArgumentCaptor<String> routingKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagePostProcessor> postProcessor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbit).convertAndSend(
            org.mockito.ArgumentMatchers.eq(OutboxMessagingConfiguration.EXCHANGE),
            routingKey.capture(), body.capture(), postProcessor.capture());
        assertThat(routingKey.getValue()).isEqualTo("video.job.completed.v1");
        JsonNode payload = json.readTree(body.getValue());
        assertThat(payload.path("eventId").asText()).isEqualTo(eventId.toString());
        assertThat(payload.path("jobId").asText()).isEqualTo(job.id().toString());
        assertThat(payload.path("type").asText()).isEqualTo("COMPLETED");
        assertThat(payload.path("recipient").asText()).isEqualTo("person@example.test");
        assertThat(payload.path("videoName").asText()).isEqualTo("aula.mp4");
        assertThat(payload.path("resultKey").asText()).isEqualTo("local-demo/" + job.id() + "/frames.zip");
        Message message = postProcessor.getValue().postProcessMessage(new Message(new byte[0], new MessageProperties()));
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
    }

    private static Job job(JobStatus status) {
        return new Job(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), JobSourceKind.VIDEO,
            "uploads/source.mp4", null, status, NOW);
    }

    private static Video video(Job job) {
        return new Video(job.videoId(), job.userId(), job.sourceKey(), "aula.mp4", "video/mp4", 1,
            "a".repeat(64), VideoStatus.UPLOADED, NOW, NOW);
    }
}
