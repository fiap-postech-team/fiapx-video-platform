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
    /** Must match the exchange the video-processor sets on {@code video.processing.v1}. */
    public static final String PROCESSING_DEAD_LETTER_EXCHANGE = "video.events.dlx";
    public static final String DEAD_LETTER_QUEUE = "video.processing.dlq.v1";
    public static final String DEAD_LETTER_ROUTING_KEY = "video.processing.dlq.v1";
    public static final String RESULT_QUEUE = "video.api.results.v1";
    public static final String RESULT_DEAD_LETTER_QUEUE = "video.api.results.dlq.v1";
    public static final String RESULT_DEAD_LETTER_ROUTING_KEY = "video.api.results.dlq.v1";

    @Bean
    Declarables videoJobTopology() {
        var exchange = new TopicExchange(EXCHANGE, true, false);
        var processingDeadLetterExchange = new TopicExchange(PROCESSING_DEAD_LETTER_EXCHANGE, true, false);
        var queue = QueueBuilder.durable(QUEUE)
            .deadLetterExchange(PROCESSING_DEAD_LETTER_EXCHANGE)
            .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
            .build();
        var deadLetterQueue = QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
        var resultQueue = QueueBuilder.durable(RESULT_QUEUE)
            .deadLetterExchange(EXCHANGE)
            .deadLetterRoutingKey(RESULT_DEAD_LETTER_ROUTING_KEY)
            .build();
        var resultDeadLetterQueue = QueueBuilder.durable(RESULT_DEAD_LETTER_QUEUE).build();
        return new Declarables(exchange, processingDeadLetterExchange, queue, deadLetterQueue,
            resultQueue, resultDeadLetterQueue,
            BindingBuilder.bind(queue).to(exchange).with("video.job.requested.v1"),
            BindingBuilder.bind(deadLetterQueue).to(processingDeadLetterExchange).with(DEAD_LETTER_ROUTING_KEY),
            BindingBuilder.bind(resultQueue).to(exchange).with("video.job.started.v1"),
            BindingBuilder.bind(resultQueue).to(exchange).with("video.job.completed.v1"),
            BindingBuilder.bind(resultQueue).to(exchange).with("video.job.failed.v1"),
            BindingBuilder.bind(resultDeadLetterQueue).to(exchange).with(RESULT_DEAD_LETTER_ROUTING_KEY));
    }
}
