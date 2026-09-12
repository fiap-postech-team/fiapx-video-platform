package br.com.fiapx.processor.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.*;

@Configuration
public class MessagingConfig {
    public static final String EXCHANGE = "video.events", QUEUE = "video.processing.v1", DLQ = "video.processing.dlq.v1";

    @Bean
    TopicExchange events() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue processing() {
        return QueueBuilder.durable(QUEUE).deadLetterExchange(EXCHANGE).deadLetterRoutingKey(DLQ).build();
    }

    @Bean
    Queue processingDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Binding requestBinding() {
        return BindingBuilder.bind(processing()).to(events()).with("video.job.requested.v1");
    }

    @Bean
    Binding dlqBinding() {
        return BindingBuilder.bind(processingDlq()).to(events()).with(DLQ);
    }
}
