package br.com.fiapx.videoprocessor.processing.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares only what this worker owns: the shared events exchange, its own work queue and the
 * dead-letter path used once retries are exhausted. Declarations are idempotent, so every service
 * may declare the shared exchange with the same arguments.
 */
@Configuration
class RabbitTopologyConfig {

    @Bean
    TopicExchange videoEventsExchange(MessagingProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    @Bean
    TopicExchange videoProcessingDeadLetterExchange(MessagingProperties properties) {
        return new TopicExchange(properties.deadLetterExchange(), true, false);
    }

    @Bean
    Queue videoProcessingQueue(MessagingProperties properties) {
        return QueueBuilder.durable(properties.queue())
                .deadLetterExchange(properties.deadLetterExchange())
                .deadLetterRoutingKey(properties.deadLetterQueue())
                .build();
    }

    @Bean
    Queue videoProcessingDeadLetterQueue(MessagingProperties properties) {
        return QueueBuilder.durable(properties.deadLetterQueue()).build();
    }

    @Bean
    Binding videoProcessingBinding(MessagingProperties properties) {
        return BindingBuilder.bind(videoProcessingQueue(properties))
                .to(videoEventsExchange(properties))
                .with(properties.routingKeys().requested());
    }

    @Bean
    Binding videoProcessingDeadLetterBinding(MessagingProperties properties) {
        return BindingBuilder.bind(videoProcessingDeadLetterQueue(properties))
                .to(videoProcessingDeadLetterExchange(properties))
                .with(properties.deadLetterQueue());
    }
}
