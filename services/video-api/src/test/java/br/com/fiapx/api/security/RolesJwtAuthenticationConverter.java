package br.com.fiapx.api.security;

import java.util.*;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Maps the signed {@code roles} claim to {@code ROLE_*} authorities. A claim
 * that is missing, has an unexpected type or carries unknown values yields no
 * authorities; roles are never derived from any other source.
 */
public class RolesJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private static final Set<String> KNOWN_ROLES = Set.of("USER", "ADMIN");

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (jwt.getClaims().get("roles") instanceof Collection<?> roles) {
            roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(role -> role.toUpperCase(Locale.ROOT))
                    .filter(KNOWN_ROLES::contains)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .forEach(authorities::add);
        }
        return new JwtAuthenticationToken(jwt, authorities);
    }
}
