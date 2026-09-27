package br.com.fiapx.notification.application.service;

import br.com.fiapx.notification.application.port.in.NotificationResult;
import br.com.fiapx.notification.application.port.in.NotifyProcessingFailureUseCase;
import br.com.fiapx.notification.application.port.out.NotificationDeliveryRepository;
import br.com.fiapx.notification.application.port.out.NotificationSender;
import br.com.fiapx.notification.application.port.out.OutboundNotification;
import br.com.fiapx.notification.domain.model.FailureNotification;
import br.com.fiapx.notification.domain.model.NotificationDelivery;
import br.com.fiapx.notification.domain.model.ProcessingOutcome;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.util.Objects;

@RequiredArgsConstructor
public final class NotifyProcessingFailureService implements NotifyProcessingFailureUseCase {

    private static final String SUCCESS_SUBJECT = "Seu vídeo foi processado";
    private static final String FAILURE_SUBJECT = "Não foi possível processar o seu vídeo";

    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationSender notificationSender;
    private final Clock clock;

    @Override
    public NotificationResult notify(FailureNotification failure) {
        Objects.requireNonNull(failure, "failure is required");

        if (deliveryRepository.existsByEventId(failure.getEventId())) {
            return NotificationResult.ALREADY_DELIVERED;
        }

        boolean succeeded = failure.getOutcome() == ProcessingOutcome.COMPLETED;
        notificationSender.send(new OutboundNotification(
                failure.getRecipient(),
                succeeded ? SUCCESS_SUBJECT : FAILURE_SUBJECT,
                body(failure.getVideoName(), succeeded)
        ));

        deliveryRepository.save(new NotificationDelivery(
                failure.getEventId(),
                failure.getJobId(),
                failure.getRecipient(),
                clock.instant()
        ));

        return NotificationResult.DELIVERED;
    }

    private static String body(String videoName, boolean succeeded) {
        if (videoName == null) {
            return succeeded
                    ? "O processamento do seu vídeo terminou. O resultado está disponível na FIAP X."
                    : "O processamento do seu vídeo não foi concluído.";
        }
        return succeeded
                ? "O processamento do vídeo \"" + videoName + "\" terminou. O resultado está disponível na FIAP X."
                : "O processamento do vídeo \"" + videoName + "\" não foi concluído.";
    }
}
