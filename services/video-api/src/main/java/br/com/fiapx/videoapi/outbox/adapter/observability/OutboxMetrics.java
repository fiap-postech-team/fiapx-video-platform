package br.com.fiapx.videoapi.outbox.adapter.observability;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class OutboxMetrics {
    private final JdbcTemplate jdbc;
    private final AtomicLong backlog = new AtomicLong();
    private final AtomicLong oldestAgeSeconds = new AtomicLong();
    private final AtomicLong failedRecords = new AtomicLong();
    private final Counter attempts;
    private final Counter published;
    private final Counter negativeConfirms;
    private final Counter expiredClaims;
    private final Timer batchDuration;

    public OutboxMetrics(MeterRegistry registry, JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        registry.gauge("outbox.backlog", backlog, AtomicLong::doubleValue);
        registry.gauge("outbox.oldest.event.age.seconds", oldestAgeSeconds, AtomicLong::doubleValue);
        registry.gauge("outbox.failed.records", failedRecords, AtomicLong::doubleValue);
        attempts = registry.counter("outbox.publish.attempts");
        published = registry.counter("outbox.published");
        negativeConfirms = registry.counter("outbox.confirms.negative");
        expiredClaims = registry.counter("outbox.claims.expired");
        batchDuration = registry.timer("outbox.batch.duration");
    }

    public void refresh() {
        var snapshot = jdbc.queryForObject("""
            select count(*) filter (where status in ('PENDING', 'PROCESSING')) as backlog,
                   coalesce(extract(epoch from (now() - min(created_at)
                       filter (where status in ('PENDING', 'PROCESSING')))), 0) as oldest_age_seconds,
                   count(*) filter (where status = 'FAILED') as failed_records
            from outbox_events
            """, (resultSet, rowNumber) -> readSnapshot(resultSet));
        if (snapshot != null) {
            backlog.set(snapshot.backlog());
            oldestAgeSeconds.set((long) snapshot.oldestAgeSeconds());
            failedRecords.set(snapshot.failedRecords());
        }
    }

    public void attempt() { attempts.increment(); }
    public void published() { published.increment(); }
    public void negativeConfirm() { negativeConfirms.increment(); }
    public void expiredClaim() { expiredClaims.increment(); }
    public void batchDuration(Duration duration) { batchDuration.record(duration); }

    private static Snapshot readSnapshot(ResultSet resultSet) throws SQLException {
        return new Snapshot(resultSet.getLong("backlog"), resultSet.getDouble("oldest_age_seconds"),
            resultSet.getLong("failed_records"));
    }

    private record Snapshot(long backlog, double oldestAgeSeconds, long failedRecords) { }
}
