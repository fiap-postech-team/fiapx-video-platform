package br.com.fiapx.videoapi.identity.adapter.out.security;

import br.com.fiapx.videoapi.identity.adapter.configuration.AuthProperties;
import br.com.fiapx.videoapi.identity.application.port.out.AccessTokenIssuer;
import br.com.fiapx.videoapi.identity.domain.IdentityUser;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Component;

@Component
public final class JwtAccessTokenIssuer implements AccessTokenIssuer {
    private final JwtEncoder encoder;
    private final AuthProperties properties;
    private final Clock clock;

    public JwtAccessTokenIssuer(JwtEncoder encoder, AuthProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedAccessToken issue(IdentityUser user, UUID sessionId) {
        var issuedAt = clock.instant();
        var expiresAt = issuedAt.plus(properties.accessTokenTtl());
        var claims = JwtClaimsSet.builder().subject(user.id().toString()).issuer(properties.issuer())
            .audience(List.of(properties.audience())).issuedAt(issuedAt).expiresAt(expiresAt)
            .id(UUID.randomUUID().toString()).claim("sid", sessionId.toString()).claim("roles", List.of(user.role().name())).build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(properties.keyId()).build();
        return new IssuedAccessToken(encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(header, claims)).getTokenValue(),
            properties.accessTokenTtl().toSeconds());
    }
}
