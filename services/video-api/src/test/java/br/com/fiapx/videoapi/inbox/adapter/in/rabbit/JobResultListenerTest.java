package br.com.fiapx.videoapi.inbox.adapter.in.rabbit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.fiapx.videoapi.inbox.application.ProcessJobResult;
import br.com.fiapx.videoapi.inbox.domain.JobResultEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class JobResultListenerTest {
    @Test
    void delegatesResultInsideTheTransactionCallback() {
        var process = mock(ProcessJobResult.class);
        var transactions = mock(TransactionTemplate.class);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var callback = (Consumer<TransactionStatus>) invocation.getArgument(0);
            callback.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactions).executeWithoutResult(any());
        var listener = new JobResultListener(new ObjectMapper(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
            process, transactions);
        var eventId = UUID.randomUUID();
        var jobId = UUID.randomUUID();

        listener.receive("""
            {"eventId":"%s","jobId":"%s","type":"PROCESSING"}
            """.formatted(eventId, jobId));

        verify(process).execute(any(JobResultEvent.class));
    }
}
