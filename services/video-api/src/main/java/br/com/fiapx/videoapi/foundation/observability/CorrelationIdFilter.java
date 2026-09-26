package br.com.fiapx.videoapi.foundation.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public final class CorrelationIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var path = request.getRequestURI();
        return path.equals("/actuator") || path.startsWith("/actuator/") || path.startsWith("/swagger-ui")
            || path.startsWith("/v3/api-docs") || path.equals("/openapi.yaml");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var previous = MDC.get(MDC_KEY);
        var correlationId = parse(request.getHeader(HEADER)).orElseGet(() -> UUID.randomUUID().toString());
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        var started = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            request.setAttribute("fiapx.errorType", exception.getClass().getSimpleName());
            request.setAttribute("fiapx.errorCode", "INTERNAL_ERROR");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw exception;
        } finally {
            var status = response.getStatus();
            if (shouldLogCompletion(request.getMethod(), status)) {
                var result = status >= 500 ? "failure" : status >= 400 ? "rejected" : "success";
                var path = normalize(request.getRequestURI());
                var durationMs = (System.nanoTime() - started) / 1_000_000.0;
                var builder = status >= 500 ? log.atError() : status >= 400 ? log.atWarn() : log.atInfo();
                builder.addKeyValue("operation", request.getMethod() + " " + path)
                    .addKeyValue("result", result)
                    .addKeyValue("httpStatus", status)
                    .addKeyValue("durationMs", durationMs);
                var errorCode = request.getAttribute("fiapx.errorCode");
                var errorType = request.getAttribute("fiapx.errorType");
                if (errorCode instanceof String code) builder.addKeyValue("errorCode", code);
                if (errorType instanceof String type) builder.addKeyValue("errorType", type);
                var jobId = request.getAttribute("fiapx.jobId");
                if (jobId instanceof String id) builder.addKeyValue("jobId", id);
                builder.log("HTTP request completed");
            }
            if (previous == null) MDC.remove(MDC_KEY); else MDC.put(MDC_KEY, previous);
        }
    }

    static boolean shouldLogCompletion(String method, int status) {
        if (status >= 400) return true;
        return !"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)
            && !"OPTIONS".equalsIgnoreCase(method);
    }

    private static java.util.Optional<String> parse(String value) {
        if (value == null || value.length() > 36) return java.util.Optional.empty();
        try {
            var parsed = UUID.fromString(value).toString();
            return parsed.equalsIgnoreCase(value) ? java.util.Optional.of(parsed) : java.util.Optional.empty();
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }

    static String normalize(String path) {
        if (path == null) return "unknown";
        return path.replaceAll("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "{id}")
            .replaceAll("(?<=/)[0-9]+(?=/|$)", "{id}");
    }
}
