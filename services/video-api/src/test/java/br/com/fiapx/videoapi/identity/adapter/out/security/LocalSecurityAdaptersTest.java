package br.com.fiapx.videoapi.identity.adapter.out.security;

import br.com.fiapx.videoapi.identity.adapter.configuration.AuthProperties;
import br.com.fiapx.videoapi.identity.adapter.in.security.LocalJwtAuthenticationConverter;
import br.com.fiapx.videoapi.identity.application.port.out.IdentityStore;
import br.com.fiapx.videoapi.identity.domain.AuthenticatedIdentity;
import br.com.fiapx.videoapi.identity.domain.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalSecurityAdaptersTest {
    @Test
    void passwordAndRefreshTokenAdaptersAvoidPlaintextAndProduceStableHash() {
        var passwords = new SpringPasswordHasher(PasswordEncoderFactories.createDelegatingPasswordEncoder());
        var tokens = new SecureRefreshTokenGenerator();
        var token = tokens.generate();

        assertThat(passwords.hash("correct-horse-battery-staple")).startsWith("{bcrypt}");
        assertThat(passwords.matches("correct-horse-battery-staple", passwords.hash("correct-horse-battery-staple"))).isTrue();
        assertThat(token).hasSize(43);
        assertThat(tokens.hash(token)).hasSize(64).isEqualTo(tokens.hash(token));
    }

    @Test
    void keyFactoryGeneratesFallbackAndDecodesConfiguredKeys() {
        var generated = new RsaKeyPairFactory(properties(null, null)).create();
        assertThat(generated.getPrivate().getAlgorithm()).isEqualTo("RSA");

        var privateKey = Base64.getEncoder().encodeToString(generated.getPrivate().getEncoded());
        var publicKey = Base64.getEncoder().encodeToString(generated.getPublic().getEncoded());
        var decoded = new RsaKeyPairFactory(properties(privateKey, publicKey)).create();
        assertThat(decoded.getPublic().getEncoded()).isEqualTo(generated.getPublic().getEncoded());
    }

    @Test
    void sessionValidatorAcceptsOnlyActiveWellFormedSession() {
        var store = mock(IdentityStore.class);
        var session = UUID.randomUUID();
        var user = UUID.randomUUID();
        when(store.hasActiveSession(session, user, Instant.EPOCH)).thenReturn(true);
        var validator = new SessionJwtValidator(store, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        assertThat(validator.validate(jwt(user.toString(), session.toString()).build()).hasErrors()).isFalse();
        assertThat(validator.validate(jwt(user.toString(), "invalid").build()).hasErrors()).isTrue();
        when(store.hasActiveSession(session, user, Instant.EPOCH)).thenReturn(false);
        assertThat(validator.validate(jwt(user.toString(), session.toString()).build()).hasErrors()).isTrue();
    }

    @Test
    void localJwtConverterMapsValidRolesAndIgnoresInvalidOnes() {
        var user = UUID.randomUUID();
        var session = UUID.randomUUID();
        var authentication = new LocalJwtAuthenticationConverter().convert(jwt(user.toString(), session.toString())
            .claim("roles", List.of("USER", "INVALID", "ADMIN")).build());

        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedIdentity.class);
        assertThat(((AuthenticatedIdentity) authentication.getPrincipal()).roles()).containsExactlyInAnyOrder(UserRole.USER, UserRole.ADMIN);
    }

    @Test
    void localJwtConverterFallsBackToEmptyRolesWhenClaimIsNotACollection() {
        var user = UUID.randomUUID();
        var session = UUID.randomUUID();
        var authentication = new LocalJwtAuthenticationConverter().convert(jwt(user.toString(), session.toString())
            .claim("roles", "USER").build());

        assertThat(authentication.getAuthorities()).isEmpty();
    }

    private AuthProperties properties(String privateKey, String publicKey) {
        return new AuthProperties("issuer", "audience", "kid", privateKey, publicKey,
            java.time.Duration.ofMinutes(15), java.time.Duration.ofDays(7), java.time.Duration.ofMinutes(15), 5);
    }

    private Jwt.Builder jwt(String subject, String session) {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject(subject).claim("sid", session)
            .issuedAt(Instant.EPOCH).expiresAt(Instant.EPOCH.plusSeconds(60));
    }
}
