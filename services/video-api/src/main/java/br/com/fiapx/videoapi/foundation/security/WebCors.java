package br.com.fiapx.videoapi.foundation.security;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

final class WebCors {
    private WebCors() {
    }

    static CorsConfigurationSource source(String allowedOrigins) {
        var origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toList();
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(Duration.ofMinutes(10));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    static CorsConfigurationSource localMockSource(String allowedOrigin) {
        var api = new CorsConfiguration();
        api.setAllowedOrigins(List.of(allowedOrigin));
        api.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        api.setAllowedHeaders(List.of("Authorization", "Content-Type", "If-None-Match",
            "x-amz-checksum-sha256", "X-XSRF-TOKEN"));
        api.setAllowCredentials(true);
        api.setMaxAge(Duration.ofMinutes(10));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/v1/**", api);
        var upload = new CorsConfiguration(api);
        upload.setAllowCredentials(false);
        source.registerCorsConfiguration("/_local/mock-storage/**", upload);
        return source;
    }
}
