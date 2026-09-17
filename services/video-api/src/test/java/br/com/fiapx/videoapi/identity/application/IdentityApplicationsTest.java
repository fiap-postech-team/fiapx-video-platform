package br.com.fiapx.videoapi.identity.application;

import br.com.fiapx.videoapi.identity.application.port.out.AccessTokenIssuer;
import br.com.fiapx.videoapi.identity.application.port.out.IdentityStore;
import br.com.fiapx.videoapi.identity.application.port.out.PasswordHasher;
import br.com.fiapx.videoapi.identity.application.port.out.RefreshTokenGenerator;
import br.com.fiapx.videoapi.identity.domain.IdentityUser;
import br.com.fiapx.videoapi.identity.domain.UserRole;
import br.com.fiapx.videoapi.identity.domain.UserStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityApplicationsTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void registerNormalizesEmailCreatesOnlyUserAndRejectsDuplicates() {
        var store = mock(IdentityStore.class);
        var passwords = mock(PasswordHasher.class);
        when(store.findUserForUpdate("person@example.test")).thenReturn(Optional.empty());
        when(passwords.hash("password-which-is-long-enough")).thenReturn("hashed");
        when(store.saveUser(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var registration = new RegisterUser(store, passwords);

        var user = registration.execute(" Person@Example.Test ", "password-which-is-long-enough");

        assertThat(user.email()).isEqualTo("person@example.test");
        assertThat(user.role()).isEqualTo(UserRole.USER);
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        when(store.findUserForUpdate("person@example.test")).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> registration.execute("person@example.test", "password-which-is-long-enough"))
            .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void loginUsesDummyHashForUnknownUserAndLocksAfterMaximumFailures() {
        var store = mock(IdentityStore.class);
        var passwords = mock(PasswordHasher.class);
        var generator = mock(RefreshTokenGenerator.class);
        var issuer = mock(AccessTokenIssuer.class);
        var policy = new AuthenticationPolicy(Duration.ofMinutes(15), Duration.ofDays(7), 5, "dummy");
        var login = new Login(store, passwords, generator, issuer, CLOCK, policy);
        when(store.findUserForUpdate(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> login.execute("unknown@example.test", "password"))
            .isInstanceOf(AuthenticationFailedException.class);
        verify(passwords).matches("password", "dummy");

        var user = user(4, null);
        when(store.findUserForUpdate("person@example.test")).thenReturn(Optional.of(user));
        when(passwords.matches("password", user.passwordHash())).thenReturn(false);
        assertThatThrownBy(() -> login.execute("person@example.test", "password"))
            .isInstanceOf(AuthenticationFailedException.class);
        verify(store).saveUser(user.failed(NOW.plus(Duration.ofMinutes(15))));
    }

    @Test
    void loginClearsFailuresAndCreatesIndependentSession() {
        var store = mock(IdentityStore.class);
        var passwords = mock(PasswordHasher.class);
        var tokens = mock(RefreshTokenGenerator.class);
        var issuer = mock(AccessTokenIssuer.class);
        var user = user(3, NOW.minusSeconds(1));
        var issued = new AccessTokenIssuer.IssuedAccessToken("access", 900);
        when(store.findUserForUpdate("person@example.test")).thenReturn(Optional.of(user));
        when(passwords.matches("password", user.passwordHash())).thenReturn(true);
        when(tokens.generate()).thenReturn("refresh");
        when(tokens.hash("refresh")).thenReturn("hash");
        when(issuer.issue(any(), any())).thenReturn(issued);
        var login = new Login(store, passwords, tokens, issuer, CLOCK,
            new AuthenticationPolicy(Duration.ofMinutes(15), Duration.ofDays(7), 5, "dummy"));

        var result = login.execute("person@example.test", "password");

        assertThat(result.accessToken()).isEqualTo(issued);
        assertThat(result.refreshExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        verify(store).saveUser(user.clearedFailures());
        verify(store).createSession(any(), any(), any());
        verify(store).createRefreshToken(any(), any(), anyString(), any(), any());
    }

    @Test
    void refreshRotatesValidTokensAndRevokesSessionOnReuse() {
        var store = mock(IdentityStore.class);
        var tokens = mock(RefreshTokenGenerator.class);
        var issuer = mock(AccessTokenIssuer.class);
        var session = UUID.randomUUID();
        var user = user(0, null);
        var valid = new IdentityStore.RefreshTokenRecord(UUID.randomUUID(), session, user.id(), NOW.plusSeconds(1), null, null);
        when(tokens.hash("valid")).thenReturn("valid-hash");
        when(store.findRefreshTokenForUpdate("valid-hash")).thenReturn(Optional.of(valid));
        when(store.findUser(user.id())).thenReturn(Optional.of(user));
        when(tokens.generate()).thenReturn("replacement");
        when(tokens.hash("replacement")).thenReturn("replacement-hash");
        when(issuer.issue(user, session)).thenReturn(new AccessTokenIssuer.IssuedAccessToken("access", 900));
        var refresh = new RefreshSession(store, tokens, issuer, CLOCK, Duration.ofDays(7));

        assertThat(refresh.execute("valid").refreshToken()).isEqualTo("replacement");
        verify(store).rotateRefreshToken(eq(valid.id()), any(), eq(session), eq("replacement-hash"),
                eq(NOW), eq(NOW.plus(Duration.ofDays(7))));

        var reused = new IdentityStore.RefreshTokenRecord(UUID.randomUUID(), session, user.id(), NOW.plusSeconds(1), NOW, null);
        when(tokens.hash("reused")).thenReturn("reused-hash");
        when(store.findRefreshTokenForUpdate("reused-hash")).thenReturn(Optional.of(reused));
        assertThatThrownBy(() -> refresh.execute("reused")).isInstanceOf(AuthenticationFailedException.class);
        verify(store).revokeSession(session, NOW);
        assertThatThrownBy(() -> refresh.execute(" ")).isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void logoutIsIdempotentAndRevokesKnownSession() {
        var store = mock(IdentityStore.class);
        var tokens = mock(RefreshTokenGenerator.class);
        var session = UUID.randomUUID();
        when(tokens.hash("refresh")).thenReturn("hash");
        when(store.findRefreshTokenForUpdate("hash")).thenReturn(Optional.of(
            new IdentityStore.RefreshTokenRecord(UUID.randomUUID(), session, UUID.randomUUID(), NOW, null, null)));
        var logout = new LogoutSession(store, tokens, CLOCK);

        logout.execute(null);
        logout.execute("refresh");

        verify(store).revokeSession(session, NOW);
    }

    private IdentityUser user(int failures, Instant lockedUntil) {
        return new IdentityUser(UUID.randomUUID(), "person@example.test", "hash", UserRole.USER, UserStatus.ACTIVE,
            failures, lockedUntil);
    }
}
