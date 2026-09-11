package br.com.fiapx.notification.infrastructure.persistence.jpa;

import br.com.fiapx.notification.application.port.out.NotificationDeliveryRepository;
import br.com.fiapx.notification.domain.model.NotificationDelivery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaNotificationDeliveryRepositoryAdapter implements NotificationDeliveryRepository {

    private final SpringDataNotificationDeliveryRepository repository;

    @Override
    public boolean existsByEventId(UUID eventId) {
        return repository.existsByEventId(eventId);
    }

    @Override
    public void save(NotificationDelivery delivery) {
        repository.save(new NotificationDeliveryEntity(
                delivery.getEventId(),
                delivery.getJobId(),
                delivery.getRecipient(),
                delivery.getDeliveredAt()
        ));
    }
}
