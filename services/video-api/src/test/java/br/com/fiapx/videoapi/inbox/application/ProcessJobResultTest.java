package br.com.fiapx.videoapi.inbox.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.fiapx.videoapi.inbox.application.port.out.InboxStore;
import br.com.fiapx.videoapi.inbox.domain.JobResultEvent;
import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessJobResultTest {

    @Test
    void appliesJobResultWhenEventIsRegistered() {
        var inbox = mock(InboxStore.class);
        var jobs = mock(JobStore.class);
        var event = event();
        when(inbox.register(event)).thenReturn(true);

        new ProcessJobResult(inbox, jobs).execute(event);

        verify(jobs).applyResult(event);
    }

    @Test
    void ignoresDuplicateEventWhenInboxAlreadyHasIt() {
        var inbox = mock(InboxStore.class);
        var jobs = mock(JobStore.class);
        var event = event();
        when(inbox.register(event)).thenReturn(false);

        new ProcessJobResult(inbox, jobs).execute(event);

        verifyNoInteractions(jobs);
    }

    private JobResultEvent event() {
        return new JobResultEvent(UUID.randomUUID(), UUID.randomUUID(), JobStatus.COMPLETED,
            "videos/result.mp4", null, Instant.EPOCH, "fingerprint");
    }
}
