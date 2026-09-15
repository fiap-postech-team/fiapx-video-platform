package br.com.fiapx.videoapi.foundation.http;

import java.net.URI;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/**
 * Creates stable RFC 9457 problem details from the foundation error catalog.
 */
@Component
public final class ApiProblemFactory {

    /**
     * Creates the stateless problem factory.
     */
    public ApiProblemFactory() {
    }

    /**
     * Creates a problem response for the supplied type and request path.
     *
     * @param type catalog entry containing safe client-facing values
     * @param instance request path without query data
     * @return fully populated problem detail
     */
    public ProblemDetail create(ProblemType type, URI instance) {
        var problem = ProblemDetail.forStatus(HttpStatusCode.valueOf(type.status()));
        problem.setDetail(type.detail());
        problem.setType(type.typeUri());
        problem.setTitle(type.title());
        problem.setInstance(instance);
        problem.setProperty("code", type.name());
        return problem;
    }
}
