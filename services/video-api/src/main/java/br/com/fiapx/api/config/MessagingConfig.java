package br.com.fiapx.api.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.*;

@Configuration
public class MessagingConfig {
    @Bean
    TopicExchange events() {
        return new TopicExchange("video.events", true, false);
    }

    @Bean
    Queue results() {
        return QueueBuilder.durable("video.api.results.v1").deadLetterExchange("video.events").deadLetterRoutingKey("video.api.results.dlq.v1").build();
    }

    @Bean
    Queue resultsDlq() {
        return QueueBuilder.durable("video.api.results.dlq.v1").build();
    }

    @Bean
    Binding started() {
        return BindingBuilder.bind(results()).to(events()).with("video.job.started.v1");
    }

    @Bean
    Binding completed() {
        return BindingBuilder.bind(results()).to(events()).with("video.job.completed.v1");
    }

    @Bean
    Binding failed() {
        return BindingBuilder.bind(results()).to(events()).with("video.job.failed.v1");
    }

    @Bean
    Binding resultDlq() {
        return BindingBuilder.bind(resultsDlq()).to(events()).with("video.api.results.dlq.v1");
    }
}
