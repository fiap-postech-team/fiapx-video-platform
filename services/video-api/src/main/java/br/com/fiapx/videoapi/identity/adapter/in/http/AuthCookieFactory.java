package br.com.fiapx.videoapi.identity.adapter.in.http;

import org.springframework.core.env.Environment;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public final class AuthCookieFactory {
    private static final String REFRESH_PATH = "/v1/auth";
    private static final String CSRF_PATH = "/";
    private final boolean secure;

    public AuthCookieFactory(Environment environment) {
        secure = !environment.matchesProfiles("local");
    }

    public ResponseCookie refresh(String value, Instant expiresAt) {
        return ResponseCookie.from("FIAPX_REFRESH", value).httpOnly(true).secure(secure).sameSite("Strict")
            .path(REFRESH_PATH).maxAge(java.time.Duration.between(Instant.now(), expiresAt)).build();
    }

    public ResponseCookie expiredRefresh() { return expired("FIAPX_REFRESH", true, REFRESH_PATH); }
    public ResponseCookie expiredCsrf() { return expired("XSRF-TOKEN", false, CSRF_PATH); }

    private ResponseCookie expired(String name, boolean httpOnly, String path) {
        return ResponseCookie.from(name, "").httpOnly(httpOnly).secure(secure).sameSite("Strict").path(path)
            .maxAge(0).build();
    }
}
