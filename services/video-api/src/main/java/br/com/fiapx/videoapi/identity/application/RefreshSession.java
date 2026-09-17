package br.com.fiapx.videoapi.identity.application;

import br.com.fiapx.videoapi.identity.application.port.out.AccessTokenIssuer;
import br.com.fiapx.videoapi.identity.application.port.out.IdentityStore;
import br.com.fiapx.videoapi.identity.application.port.out.RefreshTokenGenerator;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

public class RefreshSession {
    private final IdentityStore store;
    private final RefreshTokenGenerator tokens;
    private final AccessTokenIssuer issuer;
    private final Clock clock;
    private final Duration duration;

    public RefreshSession(IdentityStore store, RefreshTokenGenerator tokens, AccessTokenIssuer issuer,
                          Clock clock, Duration duration) {
        this.store = store; this.tokens = tokens; this.issuer = issuer; this.clock = clock; this.duration = duration;
    }

    public RefreshResult execute(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new AuthenticationFailedException();
        }
        var now = clock.instant();
        var token = store.findRefreshTokenForUpdate(tokens.hash(rawToken)).orElseThrow(AuthenticationFailedException::new);
        if (!token.canRefresh(now)) {
            revokeIfReused(token, now);
            throw new AuthenticationFailedException();
        }
        var user = store.findUser(token.userId()).orElseThrow(AuthenticationFailedException::new);
        var replacement = tokens.generate();
        var expiresAt = now.plus(duration);
        store.consumeRefreshToken(token.id(), now);
        store.createRefreshToken(UUID.randomUUID(), token.sessionId(), tokens.hash(replacement), now, expiresAt);
        return new RefreshResult(issuer.issue(user, token.sessionId()), replacement, expiresAt);
    }

    private void revokeIfReused(IdentityStore.RefreshTokenRecord token, java.time.Instant now) {
        if (token.consumedAt() != null) {
            store.revokeSession(token.sessionId(), now);
        }
    }

    public record RefreshResult(AccessTokenIssuer.IssuedAccessToken accessToken, String refreshToken,
                                java.time.Instant refreshExpiresAt) {
    }
}
