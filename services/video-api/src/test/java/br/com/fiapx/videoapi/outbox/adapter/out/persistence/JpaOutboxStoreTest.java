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

        new JpaOutboxStore(repository, new ObjectMapper()).append(eventId, jobId, userId, "uploads/source.mp4");

        var event = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(repository).save(event.capture());
        assertThat(ReflectionTestUtils.getField(event.getValue(), "id")).isEqualTo(eventId);
        assertThat(ReflectionTestUtils.getField(event.getValue(), "routingKey")).isEqualTo("video.job.requested.v1");
        assertThat((String) ReflectionTestUtils.getField(event.getValue(), "payload"))
            .contains(jobId.toString(), userId.toString(), "uploads/source.mp4");
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
