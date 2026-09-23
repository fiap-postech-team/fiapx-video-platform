package br.com.fiapx.notification.infrastructure.messaging.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RabbitMessagingConfiguration {

    public static final String EVENTS_EXCHANGE = "video.events";
    public static final String FAILURE_ROUTING_KEY = "video.job.failed.v1";
    public static final String FAILURE_QUEUE = "video.notifications.failure.v1";
    public static final String FAILURE_DLQ = "video.notifications.failure.dlq.v1";

    @Bean
    TopicExchange videoEventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    Queue failureNotificationQueue() {
        return QueueBuilder.durable(FAILURE_QUEUE)
                .deadLetterExchange(EVENTS_EXCHANGE)
                .deadLetterRoutingKey(FAILURE_DLQ)
                .build();
    }

    @Bean
    Queue failureNotificationDeadLetterQueue() {
        return QueueBuilder.durable(FAILURE_DLQ).build();
    }

    @Bean
    Binding failureNotificationBinding(
            @Qualifier("failureNotificationQueue") Queue queue,
            TopicExchange videoEventsExchange
    ) {
        return BindingBuilder.bind(queue)
                .to(videoEventsExchange)
                .with(FAILURE_ROUTING_KEY);
    }

    @Bean
    Binding failureNotificationDeadLetterBinding(
            @Qualifier("failureNotificationDeadLetterQueue") Queue deadLetterQueue,
            TopicExchange videoEventsExchange
    ) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(videoEventsExchange)
                .with(FAILURE_DLQ);
    }
}
