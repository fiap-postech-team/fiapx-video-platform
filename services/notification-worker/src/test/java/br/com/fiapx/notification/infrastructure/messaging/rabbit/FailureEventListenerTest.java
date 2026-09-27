package br.com.fiapx.notification.infrastructure.messaging.rabbit;

import br.com.fiapx.notification.application.port.in.NotificationResult;
import br.com.fiapx.notification.application.port.in.NotifyProcessingFailureUseCase;
import br.com.fiapx.notification.domain.model.FailureNotification;
import br.com.fiapx.notification.domain.model.ProcessingOutcome;
import br.com.fiapx.notification.infrastructure.config.NotificationProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FailureEventListenerTest {

    @Mock
    private NotifyProcessingFailureUseCase useCase;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void consumesProcessorJsonForSuccessAndFailure() throws Exception {
        when(useCase.notify(org.mockito.ArgumentMatchers.any())).thenReturn(NotificationResult.DELIVERED);
        FailureEventListener listener = listener();
        UUID successId = UUID.randomUUID();
        UUID failureId = UUID.randomUUID();

        listener.consume(processorMessage("""
                {"eventId":"%s","jobId":"%s","type":"COMPLETED","schemaVersion":"1.0","videoName":"aula.mp4","recipient":"person@example.test"}
                """.formatted(successId, UUID.randomUUID())));
        listener.consume(processorMessage("""
                {"eventId":"%s","jobId":"%s","type":"FAILED","schemaVersion":"1.0","reason":"The submitted video could not be decoded"}
                """.formatted(failureId, UUID.randomUUID())));

        ArgumentCaptor<FailureNotification> notifications = ArgumentCaptor.forClass(FailureNotification.class);
        verify(useCase, org.mockito.Mockito.times(2)).notify(notifications.capture());
        assertEquals(ProcessingOutcome.COMPLETED, notifications.getAllValues().get(0).getOutcome());
        assertEquals("aula.mp4", notifications.getAllValues().get(0).getVideoName());
        assertEquals("person@example.test", notifications.getAllValues().get(0).getRecipient());
        assertEquals(ProcessingOutcome.FAILED, notifications.getAllValues().get(1).getOutcome());
        assertEquals("dev@fiapx.local", notifications.getAllValues().get(1).getRecipient());
        assertEquals(null, notifications.getAllValues().get(1).getVideoName());
    }

    @Test
    void rejectsInvalidJsonWithoutNotifying() {
        FailureEventListener listener = listener();

        assertThrows(JsonProcessingException.class, () -> listener.consume(processorMessage("not-json")));

        verify(useCase, never()).notify(org.mockito.ArgumentMatchers.any());
    }

    private FailureEventListener listener() {
        return new FailureEventListener(
                objectMapper,
                validator,
                useCase,
                new NotificationProperties("dev@fiapx.local", "noreply@fiapx.local"));
    }

    private static Message processorMessage(String json) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        properties.setHeader("__TypeId__",
                "br.com.fiapx.videoprocessor.processing.infrastructure.messaging.out.JobResultMessage");
        return new Message(json.getBytes(StandardCharsets.UTF_8), properties);
    }
}
