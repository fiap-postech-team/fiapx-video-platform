package br.com.fiapx.api.outbox;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {
    private final OutboxRepository repo;
    private final RabbitTemplate rabbit;

    public OutboxPublisher(OutboxRepository r, RabbitTemplate q) {
        repo = r;
        rabbit = q;
    }

    @Scheduled(fixedDelayString = "${app.outbox.interval:1000}")
    @Transactional
    public void publish() {
        for (var e : repo.findTop100ByPublishedAtIsNullOrderByCreatedAt()) {
            rabbit.convertAndSend("video.events", e.getRoutingKey(), e.getPayload());
            e.published();
        }
    }
}
