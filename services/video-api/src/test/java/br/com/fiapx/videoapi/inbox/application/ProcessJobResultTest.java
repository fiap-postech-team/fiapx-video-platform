package br.com.fiapx.videoapi.inbox.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.fiapx.videoapi.inbox.application.port.out.InboxStore;
import br.com.fiapx.videoapi.inbox.domain.JobResultEvent;
import br.com.fiapx.videoapi.jobs.application.port.out.JobStore;
import br.com.fiapx.videoapi.jobs.domain.JobStatus;
import br.com.fiapx.videoapi.jobs.domain.Job;
import br.com.fiapx.videoapi.jobs.domain.JobSourceKind;
import br.com.fiapx.videoapi.foundation.observability.BusinessMetrics;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.Optional;
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

    @Test
    void recordsFirstTerminalTransitionAndDuration() {
        var inbox = mock(InboxStore.class);
        var jobs = mock(JobStore.class);
        var metrics = mock(BusinessMetrics.class);
        var createdAt = Instant.parse("2026-09-23T10:00:00Z");
        var event = new JobResultEvent(UUID.randomUUID(), UUID.randomUUID(), JobStatus.COMPLETED,
            "safe/result.zip", null, createdAt.plusSeconds(42), "fingerprint");
        var job = new Job(event.jobId(), UUID.randomUUID(), null, JobSourceKind.LEGACY_KEY,
            "internal-key", null, JobStatus.PROCESSING, createdAt);
        when(inbox.register(event)).thenReturn(true);
        when(jobs.findForResult(event.jobId())).thenReturn(Optional.of(job));

        new ProcessJobResult(inbox, jobs, metrics).execute(event);

        verify(jobs).applyResult(event);
        verify(metrics).jobFinished(JobStatus.COMPLETED, Duration.ofSeconds(42));
    }

    private JobResultEvent event() {
        return new JobResultEvent(UUID.randomUUID(), UUID.randomUUID(), JobStatus.COMPLETED,
            "videos/result.mp4", null, Instant.EPOCH, "fingerprint");
    }
}
