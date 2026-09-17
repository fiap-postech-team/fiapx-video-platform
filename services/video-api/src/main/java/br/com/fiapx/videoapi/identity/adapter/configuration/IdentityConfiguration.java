package br.com.fiapx.videoapi.identity.adapter.configuration;

import br.com.fiapx.videoapi.identity.adapter.in.security.LocalJwtAuthenticationConverter;
import br.com.fiapx.videoapi.identity.adapter.out.security.RsaKeyPairFactory;
import br.com.fiapx.videoapi.identity.adapter.out.security.SessionJwtValidator;
import br.com.fiapx.videoapi.identity.application.AuthenticationPolicy;
import br.com.fiapx.videoapi.identity.application.Login;
import br.com.fiapx.videoapi.identity.application.LogoutSession;
import br.com.fiapx.videoapi.identity.application.RefreshSession;
import br.com.fiapx.videoapi.identity.application.RegisterUser;
import br.com.fiapx.videoapi.identity.application.port.out.AccessTokenIssuer;
import br.com.fiapx.videoapi.identity.application.port.out.IdentityStore;
import br.com.fiapx.videoapi.identity.application.port.out.PasswordHasher;
import br.com.fiapx.videoapi.identity.application.port.out.RefreshTokenGenerator;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({AuthProperties.class, BootstrapProperties.class})
public class IdentityConfiguration {
    @Bean PasswordEncoder passwordEncoder() { return PasswordEncoderFactories.createDelegatingPasswordEncoder(); }
    @Bean KeyPair authKeyPair(RsaKeyPairFactory factory) { return factory.create(); }
    @Bean JwtEncoder jwtEncoder(KeyPair pair, AuthProperties properties) {
        var key = new RSAKey.Builder((RSAPublicKey) pair.getPublic()).privateKey(pair.getPrivate()).keyID(properties.keyId()).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }
    @Bean JwtDecoder jwtDecoder(KeyPair pair, AuthProperties properties, IdentityStore store, java.time.Clock clock) {
        var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) pair.getPublic()).build();
        var issuer = JwtValidators.createDefaultWithIssuer(properties.issuer());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, new AudienceValidator(properties.audience()), new SessionJwtValidator(store, clock)));
        return decoder;
    }
    @Bean LocalJwtAuthenticationConverter localJwtAuthenticationConverter() { return new LocalJwtAuthenticationConverter(); }
    @Bean RegisterUser registerUser(IdentityStore store, PasswordHasher passwords) { return new RegisterUser(store, passwords); }
    @Bean Login login(IdentityStore store, PasswordHasher passwords, RefreshTokenGenerator tokens, AccessTokenIssuer issuer, java.time.Clock clock, PasswordEncoder encoder, AuthProperties p) {
        return new Login(store, passwords, tokens, issuer, clock, new AuthenticationPolicy(p.lockDuration(), p.refreshTokenTtl(), p.maxFailures(), encoder.encode("invalid-password")));
    }
    @Bean RefreshSession refreshSession(IdentityStore store, RefreshTokenGenerator tokens, AccessTokenIssuer issuer, java.time.Clock clock, AuthProperties p) { return new RefreshSession(store, tokens, issuer, clock, p.refreshTokenTtl()); }
    @Bean LogoutSession logoutSession(IdentityStore store, RefreshTokenGenerator tokens, java.time.Clock clock) { return new LogoutSession(store, tokens, clock); }
}
