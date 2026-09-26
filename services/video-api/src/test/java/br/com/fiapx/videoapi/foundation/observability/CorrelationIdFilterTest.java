package br.com.fiapx.videoapi.foundation.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() { MDC.clear(); }

    @Test
    void acceptsUuidAndReturnsItWhileRestoringMdc() throws Exception {
        var correlationId = UUID.randomUUID().toString();
        var request = request();
        request.addHeader(CorrelationIdFilter.HEADER, correlationId);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> assertThat(MDC.get("correlationId")).isEqualTo(correlationId));
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo(correlationId);
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void generatesNewUuidForMissingOrInvalidHeader() throws Exception {
        for (var supplied : new String[] {null, "bad-id"}) {
            var request = request();
            if (supplied != null) request.addHeader(CorrelationIdFilter.HEADER, supplied);
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> { });
            assertThat(UUID.fromString(response.getHeader(CorrelationIdFilter.HEADER))).isNotNull();
            assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isNotEqualTo(supplied);
        }
    }

    @Test
    void restoresMdcWhenDownstreamThrows() {
        var request = request();
        var response = new MockHttpServletResponse();
        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
            throw new ServletException("secret exception message");
        })).isInstanceOf(ServletException.class);
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void normalizesUuidPathSegments() {
        assertThat(CorrelationIdFilter.normalize("/v1/jobs/123e4567-e89b-42d3-a456-426614174000"))
            .isEqualTo("/v1/jobs/{id}");
    }

    @Test
    void logsOnlyStateChangingSuccessesAndAnyHttpFailure() {
        assertThat(CorrelationIdFilter.shouldLogCompletion("GET", 200)).isFalse();
        assertThat(CorrelationIdFilter.shouldLogCompletion("HEAD", 200)).isFalse();
        assertThat(CorrelationIdFilter.shouldLogCompletion("POST", 201)).isTrue();
        assertThat(CorrelationIdFilter.shouldLogCompletion("GET", 404)).isTrue();
        assertThat(CorrelationIdFilter.shouldLogCompletion("GET", 500)).isTrue();
    }

    private MockHttpServletRequest request() {
        var request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/v1/jobs/123e4567-e89b-42d3-a456-426614174000");
        return request;
    }
}
