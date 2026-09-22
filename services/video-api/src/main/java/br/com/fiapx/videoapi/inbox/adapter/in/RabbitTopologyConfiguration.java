package br.com.fiapx.videoapi.inbox.adapter.in;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RabbitTopologyConfiguration {
    @Bean Declarables resultTopology() {
        var exchange = new TopicExchange("video.events", true, false);
        var dlq = new Queue("video.api.results.dlq.v1", true);
        var queue = new Queue("video.api.results.v1", true, false, false,
            java.util.Map.of("x-dead-letter-exchange", "video.events", "x-dead-letter-routing-key", "video.api.results.dlq.v1"));
        var bindings = new Binding[] {
            BindingBuilder.bind(queue).to(exchange).with("video.job.started.v1"),
            BindingBuilder.bind(queue).to(exchange).with("video.job.completed.v1"),
            BindingBuilder.bind(queue).to(exchange).with("video.job.failed.v1")
            ,BindingBuilder.bind(dlq).to(exchange).with("video.api.results.dlq.v1")
        };
        var all = new java.util.ArrayList<org.springframework.amqp.core.Declarable>();
        all.add(exchange); all.add(queue); all.add(dlq); all.addAll(java.util.List.of(bindings));
        return new Declarables(all);
    }
}
