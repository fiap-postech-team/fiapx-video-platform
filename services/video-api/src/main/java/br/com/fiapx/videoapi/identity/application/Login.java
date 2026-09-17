package br.com.fiapx.videoapi.identity.application;

import br.com.fiapx.videoapi.identity.application.port.out.AccessTokenIssuer;
import br.com.fiapx.videoapi.identity.application.port.out.IdentityStore;
import br.com.fiapx.videoapi.identity.application.port.out.PasswordHasher;
import br.com.fiapx.videoapi.identity.application.port.out.RefreshTokenGenerator;
import br.com.fiapx.videoapi.identity.domain.IdentityUser;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class Login {
    private final IdentityStore store;
    private final PasswordHasher passwords;
    private final RefreshTokenGenerator refreshTokens;
    private final AccessTokenIssuer accessTokens;
    private final Clock clock;
    private final Duration lockDuration;
    private final Duration refreshDuration;
    private final int maxFailures;
    private final String dummyHash;

    public Login(IdentityStore store, PasswordHasher passwords, RefreshTokenGenerator refreshTokens,
                 AccessTokenIssuer accessTokens, Clock clock, AuthenticationPolicy policy) {
        this.store = store; this.passwords = passwords; this.refreshTokens = refreshTokens;
        this.accessTokens = accessTokens; this.clock = clock; lockDuration = policy.lockDuration();
        refreshDuration = policy.refreshDuration(); maxFailures = policy.maxFailures(); dummyHash = policy.dummyHash();
    }

    public LoginResult execute(String email, String password) {
        var now = clock.instant();
        var user = store.findUserForUpdate(RegisterUser.normalize(email)).orElseGet(() -> missing(password));
        authenticate(user, password, now);
        var sessionId = UUID.randomUUID();
        var refresh = refreshTokens.generate();
        var expiresAt = now.plus(refreshDuration);
        store.createSession(sessionId, user.id(), expiresAt);
        store.createRefreshToken(UUID.randomUUID(), sessionId, refreshTokens.hash(refresh), now, expiresAt);
        return new LoginResult(user, accessTokens.issue(user, sessionId), refresh, expiresAt);
    }

    private IdentityUser missing(String password) {
        passwords.matches(password, dummyHash);
        throw new AuthenticationFailedException();
    }

    private void authenticate(IdentityUser user, String password, Instant now) {
        if (user.isLocked(now) || !passwords.matches(password, user.passwordHash())) {
            fail(user, now);
        }
        store.saveUser(user.clearedFailures());
    }

    private void fail(IdentityUser user, Instant now) {
        var failures = user.failedAttempts() + 1;
        var lockUntil = failures >= maxFailures ? now.plus(lockDuration) : null;
        store.saveUser(user.failed(lockUntil));
        throw new AuthenticationFailedException();
    }

    public record LoginResult(IdentityUser user, AccessTokenIssuer.IssuedAccessToken accessToken,
                              String refreshToken, Instant refreshExpiresAt) {
    }
}
