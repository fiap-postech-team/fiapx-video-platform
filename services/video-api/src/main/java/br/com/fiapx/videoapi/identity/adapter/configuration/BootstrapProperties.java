package br.com.fiapx.videoapi.identity.adapter.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.auth.bootstrap")
public record BootstrapProperties(boolean enabled, String email, String password) {
}
