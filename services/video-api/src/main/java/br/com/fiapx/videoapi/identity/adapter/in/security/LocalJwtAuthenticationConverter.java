package br.com.fiapx.videoapi.identity.adapter.in.security;

import br.com.fiapx.videoapi.identity.domain.AuthenticatedIdentity;
import br.com.fiapx.videoapi.identity.domain.UserRole;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public final class LocalJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    public AbstractAuthenticationToken convert(Jwt jwt) {
        var roles = roles(jwt.getClaim("roles"));
        var identity = new AuthenticatedIdentity(UUID.fromString(jwt.getSubject()), UUID.fromString(jwt.getClaimAsString("sid")), roles);
        var authorities = roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.name())).toList();
        return UsernamePasswordAuthenticationToken.authenticated(identity, jwt.getTokenValue(), authorities);
    }

    private Set<UserRole> roles(Object claim) {
        if (!(claim instanceof Collection<?> values)) {
            return Set.of();
        }
        return values.stream().filter(String.class::isInstance).map(String.class::cast).flatMap(this::role).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private java.util.stream.Stream<UserRole> role(String value) {
        try {
            return java.util.stream.Stream.of(UserRole.valueOf(value));
        } catch (IllegalArgumentException exception) {
            return java.util.stream.Stream.empty();
        }
    }
}
