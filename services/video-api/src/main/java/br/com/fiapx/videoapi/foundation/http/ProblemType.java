package br.com.fiapx.videoapi.foundation.http;

import java.net.URI;

/**
 * Catalog of stable, client-facing problem types exposed by the HTTP foundation.
 */
public enum ProblemType {
    /** Validation failed at the HTTP boundary. */
    VALIDATION_ERROR(
        "validation",
        "Requisição inválida",
        400,
        "Um ou mais dados informados são inválidos."
    ),
    /** The request does not carry an authenticated identity. */
    UNAUTHORIZED(
        "unauthorized",
        "Não autorizado",
        401,
        "Autenticação é necessária para acessar este recurso."
    ),
    AUTHENTICATION_FAILED(
        "authentication-failed",
        "Não autorizado",
        401,
        "Não foi possível autenticar as credenciais informadas."
    ),
    /** The authenticated identity is not authorized for the request. */
    ACCESS_DENIED(
        "access-denied",
        "Acesso negado",
        403,
        "Você não possui permissão para acessar este recurso."
    ),
    NOT_FOUND("not-found", "Não encontrado", 404, "Recurso não encontrado."),
    CONFLICT("conflict", "Conflito", 409, "O recurso informado já existe."),
    STORAGE_UNAVAILABLE("storage-unavailable", "Storage indisponível", 503,
        "Não foi possível acessar o armazenamento de vídeos agora."),
    /** An unexpected failure was replaced with a safe client response. */
    INTERNAL_ERROR(
        "internal-error",
        "Erro interno",
        500,
        "Não foi possível concluir a solicitação."
    );

    private final String type;
    private final String title;
    private final int status;
    private final String detail;

    ProblemType(String type, String title, int status, String detail) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
    }

    URI typeUri() {
        return URI.create("urn:fiapx:problem:" + type);
    }

    String title() {
        return title;
    }

    int status() {
        return status;
    }

    String detail() {
        return detail;
    }
}
