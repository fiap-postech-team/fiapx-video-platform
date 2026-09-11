package br.com.fiapx.notification.application.port.out;

import br.com.fiapx.notification.domain.model.NotificationDelivery;

import java.util.UUID;

public interface NotificationDeliveryRepository {
    boolean existsByEventId(UUID eventId);

    void save(NotificationDelivery delivery);
}
