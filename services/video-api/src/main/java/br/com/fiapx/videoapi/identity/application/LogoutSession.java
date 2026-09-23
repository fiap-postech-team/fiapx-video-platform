package br.com.fiapx.videoapi.identity.application;

import br.com.fiapx.videoapi.identity.application.port.out.IdentityStore;
import br.com.fiapx.videoapi.identity.application.port.out.RefreshTokenGenerator;
import java.time.Clock;

public class LogoutSession {
    private final IdentityStore store;
    private final RefreshTokenGenerator tokens;
    private final Clock clock;

    public LogoutSession(IdentityStore store, RefreshTokenGenerator tokens, Clock clock) {
        this.store = store;
        this.tokens = tokens;
        this.clock = clock;
    }

    public void execute(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        store.findRefreshTokenForUpdate(tokens.hash(rawToken))
            .ifPresent(token -> store.revokeSession(token.sessionId(), clock.instant()));
    }
}
