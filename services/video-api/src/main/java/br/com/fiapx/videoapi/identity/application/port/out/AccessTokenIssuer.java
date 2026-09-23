package br.com.fiapx.videoapi.identity.application.port.out;

import br.com.fiapx.videoapi.identity.domain.IdentityUser;
import java.util.UUID;

public interface AccessTokenIssuer {
    IssuedAccessToken issue(IdentityUser user, UUID sessionId);

    record IssuedAccessToken(String value, long expiresInSeconds) {
    }
}
