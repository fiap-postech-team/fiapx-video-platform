package br.com.fiapx.notification.application.port.out;

import jakarta.validation.Valid;

public interface NotificationSender {
    void send(@Valid OutboundNotification notification);
}
