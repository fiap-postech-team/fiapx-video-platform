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
    public static final String COMPLETED_ROUTING_KEY = "video.job.completed.v1";
    public static final String FAILURE_ROUTING_KEY = "video.job.failed.v1";
    public static final String STATUS_QUEUE = "video.notifications.status.v1";
    public static final String STATUS_DLQ = "video.notifications.status.dlq.v1";

    @Bean
    TopicExchange videoEventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    Queue statusNotificationQueue() {
        return QueueBuilder.durable(STATUS_QUEUE)
                .deadLetterExchange(EVENTS_EXCHANGE)
                .deadLetterRoutingKey(STATUS_DLQ)
                .build();
    }

    @Bean
    Queue statusNotificationDeadLetterQueue() {
        return QueueBuilder.durable(STATUS_DLQ).build();
    }

    @Bean
    Binding failedStatusNotificationBinding(
            @Qualifier("statusNotificationQueue") Queue queue,
            TopicExchange videoEventsExchange
    ) {
        return BindingBuilder.bind(queue)
                .to(videoEventsExchange)
                .with(FAILURE_ROUTING_KEY);
    }

    @Bean
    Binding completionNotificationBinding(
            @Qualifier("statusNotificationQueue") Queue queue,
            TopicExchange videoEventsExchange
    ) {
        return BindingBuilder.bind(queue)
                .to(videoEventsExchange)
                .with(COMPLETED_ROUTING_KEY);
    }

    @Bean
    Binding statusNotificationDeadLetterBinding(
            @Qualifier("statusNotificationDeadLetterQueue") Queue deadLetterQueue,
            TopicExchange videoEventsExchange
    ) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(videoEventsExchange)
                .with(STATUS_DLQ);
    }
}
