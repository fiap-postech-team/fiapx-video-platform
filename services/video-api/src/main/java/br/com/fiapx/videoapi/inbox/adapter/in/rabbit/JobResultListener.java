package br.com.fiapx.videoapi.inbox.adapter.in.rabbit;

import br.com.fiapx.videoapi.inbox.application.ProcessJobResult;
import br.com.fiapx.videoapi.inbox.domain.JobResultEvent;
import br.com.fiapx.videoapi.outbox.adapter.configuration.OutboxMessagingConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.ImmediateRequeueAmqpException;

@Component
public final class JobResultListener {
    private final JobResultMessageParser parser;
    private final ProcessJobResult process;
    private final TransactionTemplate transactions;
    private final JobResultMetrics metrics;

    @Autowired
    public JobResultListener(ObjectMapper json, Clock clock, ProcessJobResult process,
                             TransactionTemplate transactions) {
        this(json, clock, process, transactions, null);
    }

    public JobResultListener(ObjectMapper json, Clock clock, ProcessJobResult process,
                             TransactionTemplate transactions, io.micrometer.core.instrument.MeterRegistry registry) {
        this.parser = new JobResultMessageParser(json, clock);
        this.process = process;
        this.transactions = transactions;
        this.metrics = registry == null ? null : new JobResultMetrics(registry);
    }

    @RabbitListener(queues = OutboxMessagingConfiguration.RESULT_QUEUE,
        autoStartup = "${app.jobs.result-listener-enabled:true}")
    public void receive(Message message) {
        final JobResultEvent event;
        try {
            event = parser.parse(message);
            if (metrics != null) metrics.received();
        } catch (IllegalStateException exception) {
            if (metrics != null) metrics.rejected();
            throw new AmqpRejectAndDontRequeueException("Invalid job result event", exception);
        }
        try {
            transactions.executeWithoutResult(status -> process.execute(event));
            if (metrics != null) metrics.processed();
        } catch (java.util.NoSuchElementException exception) {
            long deliveries = message.getMessageProperties().isRedelivered() ? 2 : 1;
            if (deliveries < 3) {
                if (metrics != null) metrics.retry();
                throw new ImmediateRequeueAmqpException("Job not available yet", exception);
            }
            if (metrics != null) metrics.rejected();
            throw new AmqpRejectAndDontRequeueException("Unknown job", exception);
        }
    }

    /** Compatibility entry point for direct callers and legacy tests. */
    public void receive(String payload) {
        var properties = new org.springframework.amqp.core.MessageProperties();
        properties.setReceivedRoutingKey(null);
        receive(new Message(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8), properties));
    }
}
