package br.com.fiapx.videoapi.jobs.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JobCursorTest {

    @Test
    void roundTripsTheCursorBoundary() {
        var original = new JobCursor(Instant.parse("2026-09-17T00:00:00Z"), UUID.randomUUID());

        assertThat(JobCursor.decode(original.encode())).isEqualTo(original);
    }

    @Test
    void rejectsMalformedCursors() {
        assertThatThrownBy(() -> JobCursor.decode("not-a-cursor"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
