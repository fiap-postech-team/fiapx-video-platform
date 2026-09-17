package br.com.fiapx.videoapi.identity.adapter.in.http;

import br.com.fiapx.videoapi.identity.adapter.configuration.AuthProperties;
import java.time.Instant;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public final class AuthCookieFactory {
    private static final String PATH = "/v1/auth";
    private final boolean secure;

    public AuthCookieFactory(Environment environment) {
        secure = !environment.matchesProfiles("local");
    }

    public ResponseCookie refresh(String value, Instant expiresAt) {
        return ResponseCookie.from("FIAPX_REFRESH", value).httpOnly(true).secure(secure).sameSite("Strict")
            .path(PATH).maxAge(java.time.Duration.between(Instant.now(), expiresAt)).build();
    }

    public ResponseCookie expiredRefresh() { return expired("FIAPX_REFRESH", true); }
    public ResponseCookie expiredCsrf() { return expired("XSRF-TOKEN", false); }

    private ResponseCookie expired(String name, boolean httpOnly) {
        return ResponseCookie.from(name, "").httpOnly(httpOnly).secure(secure).sameSite("Strict").path(PATH)
            .maxAge(0).build();
    }
}
