package br.com.fiapx.videoapi.foundation.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class WebCorsTest {
    @Test
    void allowsConfiguredOriginsWithCredentialsAndRequiredHeaders() {
        var source = WebCors.source("http://localhost:5173, http://127.0.0.1:5173");
        var request = new MockHttpServletRequest("OPTIONS", "/v1/auth/login");
        request.addHeader("Origin", "http://localhost:5173");
        var configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).containsExactly("http://localhost:5173", "http://127.0.0.1:5173");
        assertThat(configuration.getAllowCredentials()).isTrue();
        assertThat(configuration.getAllowedHeaders()).contains("Authorization", "Content-Type", "X-XSRF-TOKEN");
        assertThat(configuration.getAllowedOrigins()).doesNotContain("*");
    }

    @Test
    void doesNotAdvertiseAnyOriginWhenNoneAreConfigured() {
        var source = WebCors.source(" ");
        var request = new MockHttpServletRequest("GET", "/v1/me");
        var configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).isEmpty();
    }

    @Test
    void localMockAllowsEachConfiguredOriginAndDropsCredentialsOnUpload() {
        var source = WebCors.localMockSource("http://localhost:5173, http://localhost:5174");
        var login = new MockHttpServletRequest("OPTIONS", "/v1/auth/login");
        login.addHeader("Origin", "http://localhost:5174");
        var api = source.getCorsConfiguration(login);

        assertThat(api).isNotNull();
        assertThat(api.getAllowedOrigins()).containsExactly("http://localhost:5173", "http://localhost:5174");
        assertThat(api.getAllowCredentials()).isTrue();

        var upload = new MockHttpServletRequest("PUT", "/_local/mock-storage/uploads/object");
        upload.addHeader("Origin", "http://localhost:5174");
        var storage = source.getCorsConfiguration(upload);

        assertThat(storage).isNotNull();
        assertThat(storage.getAllowCredentials()).isFalse();
        assertThat(storage.getAllowedOrigins()).containsExactly("http://localhost:5173", "http://localhost:5174");
    }
}
