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

@Component
public final class JobResultListener {
    private final JobResultMessageParser parser;
    private final ProcessJobResult process;
    private final TransactionTemplate transactions;

    public JobResultListener(ObjectMapper json, Clock clock, ProcessJobResult process,
                             TransactionTemplate transactions) {
        this.parser = new JobResultMessageParser(json, clock);
        this.process = process;
        this.transactions = transactions;
    }

    @RabbitListener(queues = OutboxMessagingConfiguration.RESULT_QUEUE,
        autoStartup = "${app.jobs.result-listener-enabled:true}")
    public void receive(String payload) {
        final JobResultEvent event;
        try {
            event = parser.parse(payload);
        } catch (IllegalStateException exception) {
            throw new AmqpRejectAndDontRequeueException("Invalid job result event", exception);
        }
        transactions.executeWithoutResult(status -> process.execute(event));
    }
}
