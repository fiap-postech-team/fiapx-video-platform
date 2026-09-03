package br.com.fiapx.notification;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationDelivery, UUID> {
    boolean existsByEventId(UUID eventId);
}
