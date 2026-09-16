package br.com.fiapx.videoapi.identity.adapter.out.security;

import br.com.fiapx.videoapi.identity.application.port.out.IdentityStore;
import java.time.Clock;
import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public final class SessionJwtValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID = new OAuth2Error("invalid_token", "Invalid local session", null);
    private final IdentityStore store;
    private final Clock clock;

    public SessionJwtValidator(IdentityStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            var sessionId = UUID.fromString(jwt.getClaimAsString("sid"));
            var userId = UUID.fromString(jwt.getSubject());
            return store.hasActiveSession(sessionId, userId, clock.instant())
                ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(INVALID);
        } catch (IllegalArgumentException exception) {
            return OAuth2TokenValidatorResult.failure(INVALID);
        }
    }
}
