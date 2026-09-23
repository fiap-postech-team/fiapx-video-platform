package br.com.fiapx.videoapi.inbox.application.port.out;

import br.com.fiapx.videoapi.inbox.domain.JobResultEvent;

public interface InboxStore {
    boolean register(JobResultEvent event);
}
