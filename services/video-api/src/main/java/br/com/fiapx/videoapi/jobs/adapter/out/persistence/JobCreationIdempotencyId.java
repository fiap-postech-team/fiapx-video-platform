package br.com.fiapx.videoapi.jobs.adapter.out.persistence;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
class JobCreationIdempotencyId implements Serializable {
    private UUID userId;
    private String idempotencyKey;

    protected JobCreationIdempotencyId() {
    }

    JobCreationIdempotencyId(UUID userId, String idempotencyKey) {
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof JobCreationIdempotencyId that)) {
            return false;
        }
        return userId.equals(that.userId) && idempotencyKey.equals(that.idempotencyKey);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userId, idempotencyKey);
    }
}
