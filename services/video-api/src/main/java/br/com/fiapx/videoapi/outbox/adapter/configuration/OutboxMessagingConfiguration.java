package br.com.fiapx.videoapi.outbox.adapter.configuration;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxMessagingConfiguration {
    public static final String EXCHANGE = "video.events";
    public static final String QUEUE = "video.processing.v1";
    public static final String DEAD_LETTER_QUEUE = "video.processing.dlq.v1";
    public static final String DEAD_LETTER_ROUTING_KEY = "video.processing.dlq.v1";

    @Bean
    Declarables videoJobTopology() {
        var exchange = new TopicExchange(EXCHANGE, true, false);
        var queue = QueueBuilder.durable(QUEUE)
            .deadLetterExchange(EXCHANGE)
            .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
            .build();
        var deadLetterQueue = QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
        return new Declarables(exchange, queue, deadLetterQueue,
            BindingBuilder.bind(queue).to(exchange).with("video.job.requested.v1"),
            BindingBuilder.bind(deadLetterQueue).to(exchange).with(DEAD_LETTER_ROUTING_KEY));
    }
}
