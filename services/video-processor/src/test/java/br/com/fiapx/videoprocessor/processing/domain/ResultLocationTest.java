package br.com.fiapx.videoprocessor.processing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResultLocationTest {

    @Test
    void derivesTheKeyOnlyFromTheJobId() {
        UUID jobId = UUID.fromString("a96aa430-b106-4dcc-b340-03adfc1cb51b");

        assertThat(ResultLocation.forJob(jobId).key())
                .isEqualTo("results/a96aa430-b106-4dcc-b340-03adfc1cb51b/frames.zip");
    }

    @Test
    void producesTheSameKeyForRepeatedDeliveriesOfTheSameJob() {
        UUID jobId = UUID.randomUUID();

        assertThat(ResultLocation.forJob(jobId)).isEqualTo(ResultLocation.forJob(jobId));
    }

    @Test
    void rejectsABlankKey() {
        assertThatThrownBy(() -> new ResultLocation("  ")).isInstanceOf(IllegalArgumentException.class);
    }
}
