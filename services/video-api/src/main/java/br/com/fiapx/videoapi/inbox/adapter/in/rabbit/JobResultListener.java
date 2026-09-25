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
import br.com.fiapx.videoapi.foundation.observability.BusinessMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Component
public final class JobResultListener {
    private static final Logger log = LoggerFactory.getLogger(JobResultListener.class);
    private final JobResultMessageParser parser;
    private final ProcessJobResult process;
    private final TransactionTemplate transactions;
    private final JobResultMetrics metrics;
    private final BusinessMetrics businessMetrics;

    @Autowired
    public JobResultListener(ObjectMapper json, Clock clock, ProcessJobResult process,
                             TransactionTemplate transactions, MeterRegistry registry,
                             BusinessMetrics businessMetrics) {
        this(json, clock, process, transactions, registry, businessMetrics, true);
    }

    public JobResultListener(ObjectMapper json, Clock clock, ProcessJobResult process,
                             TransactionTemplate transactions) {
        this(json, clock, process, transactions, null, null, true);
    }

    public JobResultListener(ObjectMapper json, Clock clock, ProcessJobResult process,
                             TransactionTemplate transactions, io.micrometer.core.instrument.MeterRegistry registry) {
        this(json, clock, process, transactions, registry, null, true);
    }

    private JobResultListener(ObjectMapper json, Clock clock, ProcessJobResult process,
                              TransactionTemplate transactions, MeterRegistry registry,
                              BusinessMetrics businessMetrics, boolean ignored) {
        this.parser = new JobResultMessageParser(json, clock);
        this.process = process;
        this.transactions = transactions;
        this.metrics = registry == null ? null : new JobResultMetrics(registry);
        this.businessMetrics = businessMetrics;
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
            if (businessMetrics != null) businessMetrics.consumptionFailure("INVALID_MESSAGE");
            throw new AmqpRejectAndDontRequeueException("Invalid job result event", exception);
        }
        var previousCorrelation = MDC.get("correlationId");
        var previousJob = MDC.get("jobId");
        MDC.put("correlationId", event.correlationId().toString());
        MDC.put("jobId", event.jobId().toString());
        try {
            transactions.executeWithoutResult(status -> process.execute(event));
            if (metrics != null) metrics.processed();
        } catch (java.util.NoSuchElementException exception) {
            long deliveries = message.getMessageProperties().isRedelivered() ? 2 : 1;
            if (deliveries < 3) {
                if (metrics != null) metrics.retry();
                if (businessMetrics != null) businessMetrics.consumptionFailure("RETRY");
                throw new ImmediateRequeueAmqpException("Job not available yet", exception);
            }
            if (metrics != null) metrics.rejected();
            if (businessMetrics != null) businessMetrics.consumptionFailure("REJECTED");
            throw new AmqpRejectAndDontRequeueException("Unknown job", exception);
        } catch (RuntimeException exception) {
            if (businessMetrics != null) businessMetrics.consumptionFailure("RETRY");
            log.atWarn().addKeyValue("operation", "job-result.consume")
                .addKeyValue("result", "retry")
                .addKeyValue("errorType", exception.getClass().getSimpleName())
                .log("Job result consumption failed");
            throw exception;
        } finally {
            restore("jobId", previousJob);
            restore("correlationId", previousCorrelation);
        }
    }

    private static void restore(String key, String value) {
        if (value == null) MDC.remove(key); else MDC.put(key, value);
    }

    /** Compatibility entry point for direct callers and legacy tests. */
    public void receive(String payload) {
        var properties = new org.springframework.amqp.core.MessageProperties();
        properties.setReceivedRoutingKey(null);
        receive(new Message(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8), properties));
    }
}
