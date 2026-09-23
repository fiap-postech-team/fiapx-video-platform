package br.com.fiapx.api.outbox;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {
    private final OutboxRepository repo;
    private final RabbitTemplate rabbit;

    public OutboxPublisher(OutboxRepository repo, RabbitTemplate rabbit) {
        this.repo = repo;
        this.rabbit = rabbit;
    }

    public void publish() {
        for (OutboxEvent event : repo.findTop100ByPublishedAtIsNullOrderByCreatedAt()) {
            rabbit.convertAndSend("video.events", event.getRoutingKey(), event.getPayload());
            event.published();
        }
    }
}
