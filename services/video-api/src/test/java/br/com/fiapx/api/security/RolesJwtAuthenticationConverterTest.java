package br.com.fiapx.api.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class RolesJwtAuthenticationConverterTest {
    private final RolesJwtAuthenticationConverter converter = new RolesJwtAuthenticationConverter();

    private Jwt jwtWithClaim(Object roles) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("00000000-0000-0000-0000-000000000001");
        if (roles != null) {
            builder.claim("roles", roles);
        }
        return builder.build();
    }

    @Test
    void mapsKnownRolesToAuthorities() {
        var auth = converter.convert(jwtWithClaim(List.of("USER", "ADMIN")));
        assertThat(auth.getAuthorities())
                .containsExactlyInAnyOrder(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test
    void normalizesRoleCase() {
        var auth = converter.convert(jwtWithClaim(List.of("admin")));
        assertThat(auth.getAuthorities()).containsExactly(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test
    void missingClaimYieldsNoAuthorities() {
        assertThat(converter.convert(jwtWithClaim(null)).getAuthorities()).isEmpty();
    }

    @Test
    void claimWithUnexpectedTypeYieldsNoAuthorities() {
        assertThat(converter.convert(jwtWithClaim("ADMIN")).getAuthorities()).isEmpty();
    }

    @Test
    void unknownRoleValuesAreIgnored() {
        assertThat(converter.convert(jwtWithClaim(List.of("SUPERUSER", "ROLE_ADMIN"))).getAuthorities()).isEmpty();
    }

    @Test
    void nonStringEntriesAreIgnored() {
        var auth = converter.convert(jwtWithClaim(List.of("USER", 42)));
        assertThat(auth.getAuthorities()).containsExactly(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
