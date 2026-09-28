package br.com.fiapx.notification.infrastructure.messaging.rabbit;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMessagingConfigurationTest {

    private final RabbitMessagingConfiguration configuration = new RabbitMessagingConfiguration();

    @Test
    void routesCompletedAndFailedEventsToTheDedicatedStatusQueue() {
        TopicExchange exchange = configuration.videoEventsExchange();
        Queue queue = configuration.statusNotificationQueue();
        Binding completed = configuration.completionNotificationBinding(queue, exchange);
        Binding failed = configuration.failedStatusNotificationBinding(queue, exchange);

        assertThat(exchange.getName()).isEqualTo("video.events");
        assertThat(queue.getName()).isEqualTo("video.notifications.status.v1");
        assertThat(completed.getDestination()).isEqualTo(queue.getName());
        assertThat(failed.getDestination()).isEqualTo(queue.getName());
        assertThat(completed.getRoutingKey()).isEqualTo("video.job.completed.v1");
        assertThat(failed.getRoutingKey()).isEqualTo("video.job.failed.v1");
    }

    @Test
    void routesRejectedStatusMessagesToTheirOwnDeadLetterQueue() {
        TopicExchange exchange = configuration.videoEventsExchange();
        Queue statusQueue = configuration.statusNotificationQueue();
        Queue deadLetterQueue = configuration.statusNotificationDeadLetterQueue();
        Binding deadLetterBinding = configuration.statusNotificationDeadLetterBinding(deadLetterQueue, exchange);

        assertThat(statusQueue.getArguments())
                .containsEntry("x-dead-letter-exchange", "video.events")
                .containsEntry("x-dead-letter-routing-key", "video.notifications.status.dlq.v1");
        assertThat(deadLetterQueue.getName()).isEqualTo("video.notifications.status.dlq.v1");
        assertThat(deadLetterBinding.getDestination()).isEqualTo(deadLetterQueue.getName());
        assertThat(deadLetterBinding.getRoutingKey()).isEqualTo(deadLetterQueue.getName());
    }
}
