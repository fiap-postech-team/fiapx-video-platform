package br.com.fiapx.notification.application.service;

import br.com.fiapx.notification.application.port.in.NotificationResult;
import br.com.fiapx.notification.application.port.in.NotifyProcessingFailureUseCase;
import br.com.fiapx.notification.application.port.out.NotificationDeliveryRepository;
import br.com.fiapx.notification.application.port.out.NotificationSender;
import br.com.fiapx.notification.application.port.out.OutboundNotification;
import br.com.fiapx.notification.domain.model.FailureNotification;
import br.com.fiapx.notification.domain.model.NotificationDelivery;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.util.Objects;

@RequiredArgsConstructor
public final class NotifyProcessingFailureService implements NotifyProcessingFailureUseCase {

    private static final String FAILURE_SUBJECT = "Falha no processamento do vídeo";

    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationSender notificationSender;
    private final Clock clock;

    @Override
    public NotificationResult notify(FailureNotification failure) {
        Objects.requireNonNull(failure, "failure is required");

        if (deliveryRepository.existsByEventId(failure.getEventId())) {
            return NotificationResult.ALREADY_DELIVERED;
        }

        notificationSender.send(new OutboundNotification(
                failure.getRecipient(),
                FAILURE_SUBJECT,
                "O job " + failure.getJobId() + " falhou: " + failure.getReason()
        ));

        deliveryRepository.save(new NotificationDelivery(
                failure.getEventId(),
                failure.getJobId(),
                failure.getRecipient(),
                clock.instant()
        ));

        return NotificationResult.DELIVERED;
    }
}
