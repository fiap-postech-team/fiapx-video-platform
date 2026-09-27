package br.com.fiapx.notification.application.service;

import br.com.fiapx.notification.application.port.in.NotificationResult;
import br.com.fiapx.notification.application.port.out.NotificationDeliveryRepository;
import br.com.fiapx.notification.application.port.out.NotificationSender;
import br.com.fiapx.notification.application.port.out.OutboundNotification;
import br.com.fiapx.notification.domain.model.FailureNotification;
import br.com.fiapx.notification.domain.model.NotificationDelivery;
import br.com.fiapx.notification.domain.model.ProcessingOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyProcessingFailureServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-09T20:00:00Z");

    @Mock
    private NotificationDeliveryRepository repository;

    @Mock
    private NotificationSender sender;

    private NotifyProcessingFailureService service;

    @BeforeEach
    void setUp() {
        service = new NotifyProcessingFailureService(
                repository,
                sender,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldSendAndPersistWhenEventHasNotBeenDelivered() {
        FailureNotification failure = failure();
        when(repository.existsByEventId(failure.getEventId())).thenReturn(false);

        NotificationResult result = service.notify(failure);

        assertEquals(NotificationResult.DELIVERED, result);

        ArgumentCaptor<OutboundNotification> messageCaptor = ArgumentCaptor.forClass(OutboundNotification.class);
        verify(sender).send(messageCaptor.capture());
        assertEquals(failure.getRecipient(), messageCaptor.getValue().recipient());
        assertEquals("Não foi possível processar o seu vídeo", messageCaptor.getValue().subject());
        assertEquals("O processamento do seu vídeo não foi concluído.", messageCaptor.getValue().body());
        org.junit.jupiter.api.Assertions.assertFalse(messageCaptor.getValue().body().contains(failure.getJobId().toString()));

        verify(repository).save(new NotificationDelivery(
                failure.getEventId(),
                failure.getJobId(),
                failure.getRecipient(),
                NOW
        ));
    }

    @Test
    void shouldNameTheVideoInSuccessAndFailureEmails() {
        when(repository.existsByEventId(any())).thenReturn(false);
        FailureNotification success = new FailureNotification(
                UUID.randomUUID(), UUID.randomUUID(), "student@example.com", null, "aula.mp4", ProcessingOutcome.COMPLETED);
        FailureNotification failure = new FailureNotification(
                UUID.randomUUID(), UUID.randomUUID(), "student@example.com", "timeout", "aula.mp4", ProcessingOutcome.FAILED);

        service.notify(success);
        service.notify(failure);

        ArgumentCaptor<OutboundNotification> messages = ArgumentCaptor.forClass(OutboundNotification.class);
        verify(sender, org.mockito.Mockito.times(2)).send(messages.capture());
        assertEquals("Seu vídeo foi processado", messages.getAllValues().get(0).subject());
        assertEquals(
                "O processamento do vídeo \"aula.mp4\" terminou. O resultado está disponível na FIAP X.",
                messages.getAllValues().get(0).body());
        assertEquals("Não foi possível processar o seu vídeo", messages.getAllValues().get(1).subject());
        assertEquals(
                "O processamento do vídeo \"aula.mp4\" não foi concluído.",
                messages.getAllValues().get(1).body());
        org.junit.jupiter.api.Assertions.assertFalse(messages.getAllValues().get(1).body().contains("timeout"));
    }

    @Test
    void shouldIgnoreEventThatWasAlreadyDelivered() {
        FailureNotification failure = failure();
        when(repository.existsByEventId(failure.getEventId())).thenReturn(true);

        NotificationResult result = service.notify(failure);

        assertEquals(NotificationResult.ALREADY_DELIVERED, result);
        verifyNoInteractions(sender);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldNotPersistDeliveryWhenSenderFails() {
        FailureNotification failure = failure();
        when(repository.existsByEventId(failure.getEventId())).thenReturn(false);
        doThrow(new IllegalStateException("smtp unavailable")).when(sender).send(any());

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> service.notify(failure)
        );

        verify(repository, never()).save(any());
    }

    private FailureNotification failure() {
        return new FailureNotification(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "student@example.com",
                "timeout"
        );
    }
}
