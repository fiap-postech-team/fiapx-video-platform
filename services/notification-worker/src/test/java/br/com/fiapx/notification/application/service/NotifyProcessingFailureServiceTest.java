package br.com.fiapx.notification.application.service;

import br.com.fiapx.notification.application.port.in.NotificationResult;
import br.com.fiapx.notification.application.port.out.NotificationDeliveryRepository;
import br.com.fiapx.notification.application.port.out.NotificationSender;
import br.com.fiapx.notification.application.port.out.OutboundNotification;
import br.com.fiapx.notification.domain.model.FailureNotification;
import br.com.fiapx.notification.domain.model.NotificationDelivery;
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
        assertEquals("Falha no processamento do vídeo", messageCaptor.getValue().subject());
        assertEquals("O job " + failure.getJobId() + " falhou: timeout", messageCaptor.getValue().body());

        verify(repository).save(new NotificationDelivery(
                failure.getEventId(),
                failure.getJobId(),
                failure.getRecipient(),
                NOW
        ));
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
