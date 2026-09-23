package br.com.fiapx.videoapi.inbox.application.port.out;

import br.com.fiapx.videoapi.inbox.domain.JobResultEvent;

public interface InboxStore {
    boolean register(JobResultEvent event);
    default void complete(JobResultEvent event, String reason) { }
}
