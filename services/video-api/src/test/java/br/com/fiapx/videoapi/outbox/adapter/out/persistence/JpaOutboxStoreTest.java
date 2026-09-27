package br.com.fiapx.videoapi.outbox.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class JpaOutboxStoreTest {

    @Test
    void persistsRequestedEventWithCanonicalRoutingKeyAndPayload() {
        var repository = mock(SpringDataOutboxRepository.class);
        var eventId = UUID.randomUUID();
        var jobId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var videoId = UUID.randomUUID();
        var correlationId = UUID.randomUUID();
        var occurredAt = Instant.parse("2026-09-22T12:00:00Z");

        new JpaOutboxStore(repository, new ObjectMapper()).append(eventId, jobId, userId, videoId,
            "uploads/source.mp4", occurredAt, correlationId);

        var event = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(repository).save(event.capture());
        assertThat(ReflectionTestUtils.getField(event.getValue(), "id")).isEqualTo(eventId);
        assertThat(ReflectionTestUtils.getField(event.getValue(), "routingKey")).isEqualTo("video.job.requested.v1");
        assertThat((String) ReflectionTestUtils.getField(event.getValue(), "payload"))
            .contains(jobId.toString(), userId.toString(), "uploads/source.mp4");
        assertThat((Map<String, Object>) ReflectionTestUtils.getField(event.getValue(), "payloadJson"))
            .containsEntry("type", "video.job.requested.v1")
            .containsEntry("schemaVersion", 1)
            .containsEntry("correlationId", correlationId)
            .containsEntry("occurredAt", occurredAt.toString())
            .doesNotContainKey("recipient")
            .doesNotContainKey("videoName");
    }

    @Test
    void includesRecipientAndVideoNameWhenTheJobKnowsThem() {
        var repository = mock(SpringDataOutboxRepository.class);
        var occurredAt = Instant.parse("2026-09-22T12:00:00Z");

        new JpaOutboxStore(repository, new ObjectMapper()).append(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "uploads/source.mp4", occurredAt, UUID.randomUUID(), " person@example.test ", " aula.mp4 ");

        var event = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(repository).save(event.capture());
        assertThat((Map<String, Object>) ReflectionTestUtils.getField(event.getValue(), "payloadJson"))
            .containsEntry("recipient", "person@example.test")
            .containsEntry("videoName", "aula.mp4");
    }

    @Test
    void claimsEventsAndRecoversAnExpiredClaim() {
        var repository = mock(SpringDataOutboxRepository.class);
        var eventId = UUID.randomUUID();
        var jobId = UUID.randomUUID();
        var at = Instant.parse("2026-09-22T12:00:00Z");
        var payload = Map.<String, Object>of("jobId", jobId);
        var event = new OutboxEventEntity(eventId, jobId, "{}", payload, at);
        when(repository.lockReady(at, 1)).thenReturn(List.of(event));

        var claim = new JpaOutboxStore(repository, new ObjectMapper())
            .claimReady(1, "api-1", at, at.plusSeconds(30)).getFirst();

        assertThat(claim.eventId()).isEqualTo(eventId);
        assertThat(claim.jobId()).isEqualTo(jobId);
        assertThat(claim.claimToken()).isNotNull();
        assertThat(claim.attempts()).isEqualTo(1);
        assertThat(claim.recovered()).isFalse();
    }

    @Test
    void delegatesConditionalStateChangesToRepository() {
        var repository = mock(SpringDataOutboxRepository.class);
        var eventId = UUID.randomUUID();
        var claimToken = UUID.randomUUID();
        when(repository.markPublished(eventId, claimToken, Instant.EPOCH)).thenReturn(1);
        when(repository.scheduleRetry(eventId, claimToken, Instant.EPOCH, "NACK")).thenReturn(1);
        when(repository.markFailed(eventId, claimToken, "TIMEOUT")).thenReturn(1);
        var store = new JpaOutboxStore(repository, new ObjectMapper());

        assertThat(store.markPublished(eventId, claimToken, Instant.EPOCH)).isTrue();
        assertThat(store.scheduleRetry(eventId, claimToken, Instant.EPOCH, "NACK")).isTrue();
        assertThat(store.markFailed(eventId, claimToken, "TIMEOUT")).isTrue();
    }

    @Test
    void translatesSerializationFailuresToApplicationFailure() {
        var repository = mock(SpringDataOutboxRepository.class);
        var failingMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw JsonMappingException.fromUnexpectedIOE(new java.io.IOException("serialization failed"));
            }
        };

        assertThatThrownBy(() -> new JpaOutboxStore(repository, failingMapper)
            .append(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "uploads/source.mp4"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot serialize job event");
    }
}
