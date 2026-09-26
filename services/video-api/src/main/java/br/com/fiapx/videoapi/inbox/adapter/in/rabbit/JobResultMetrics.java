package br.com.fiapx.videoapi.inbox.adapter.in.rabbit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

final class JobResultMetrics {
    private final Counter received;
    private final Counter processed;
    private final Counter rejected;
    private final Counter retries;
    JobResultMetrics(MeterRegistry registry) {
        received = registry.counter("video_api_job_results_received_total");
        processed = registry.counter("video_api_job_results_processed_total");
        rejected = registry.counter("video_api_job_results_dlq_total");
        retries = registry.counter("video_api_job_results_retries_total");
    }
    void received() { received.increment(); }
    void processed() { processed.increment(); }
    void rejected() { rejected.increment(); }
    void retry() { retries.increment(); }
}
