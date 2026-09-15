package br.com.fiapx.videoapi.foundation.http;

import java.net.URI;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public final class ApiProblemFactory {

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
