package br.com.fiapx.notification.application.port.in;

import br.com.fiapx.notification.domain.model.FailureNotification;

public interface NotifyProcessingFailureUseCase {
    NotificationResult notify(FailureNotification failure);
}
