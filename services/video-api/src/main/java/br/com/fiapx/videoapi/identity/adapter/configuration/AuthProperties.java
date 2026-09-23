package br.com.fiapx.videoapi.identity.adapter.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.auth")
public record AuthProperties(String issuer, String audience, String keyId, String privateKeyBase64,
                             String publicKeyBase64, Duration accessTokenTtl, Duration refreshTokenTtl,
                             Duration lockDuration, int maxFailures) {
}
