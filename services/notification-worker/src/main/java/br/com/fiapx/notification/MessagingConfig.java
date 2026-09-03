package br.com.fiapx.notification;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.*;

@Configuration
public class MessagingConfig {
    @Bean
    TopicExchange events() {
        return new TopicExchange("video.events", true, false);
    }

    @Bean
    Queue failureQueue() {
        return QueueBuilder.durable("video.notifications.failure.v1").deadLetterExchange("video.events").deadLetterRoutingKey("video.notifications.failure.dlq.v1").build();
    }

    @Bean
    Queue failureDlq() {
        return QueueBuilder.durable("video.notifications.failure.dlq.v1").build();
    }

    @Bean
    Binding failureBinding() {
        return BindingBuilder.bind(failureQueue()).to(events()).with("video.job.failed.v1");
    }

    @Bean
    Binding failureDlqBinding() {
        return BindingBuilder.bind(failureDlq()).to(events()).with("video.notifications.failure.dlq.v1");
    }
}
