package br.com.fiapx.videoapi.identity.domain;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedIdentity(UUID userId, UUID sessionId, Set<UserRole> roles) {
}
