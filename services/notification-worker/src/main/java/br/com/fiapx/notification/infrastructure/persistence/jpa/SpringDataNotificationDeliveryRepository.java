package br.com.fiapx.notification.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataNotificationDeliveryRepository extends JpaRepository<NotificationDeliveryEntity, UUID> {
    boolean existsByEventId(UUID eventId);
}
