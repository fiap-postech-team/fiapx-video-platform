package br.com.fiapx.videoprocessor.processing.infrastructure.messaging.out;

import br.com.fiapx.videoprocessor.processing.application.port.out.JobEventPublisher;
import br.com.fiapx.videoprocessor.processing.domain.JobEvent;
import br.com.fiapx.videoprocessor.processing.infrastructure.messaging.MessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitJobEventPublisher implements JobEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitJobEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties properties;

    public RabbitJobEventPublisher(RabbitTemplate rabbitTemplate, MessagingProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(JobEvent event) {
        String routingKey = properties.routingKeys().forEventType(event.type());
        rabbitTemplate.convertAndSend(properties.exchange(), routingKey, JobResultMessage.from(event), message -> {
            message.getMessageProperties().setMessageId(event.eventId().toString());
            message.getMessageProperties().setCorrelationId(event.correlationId().toString());
            return message;
        });
        log.debug("published {} for job {} with routing key {}", event.type(), event.jobId(), routingKey);
    }
}
