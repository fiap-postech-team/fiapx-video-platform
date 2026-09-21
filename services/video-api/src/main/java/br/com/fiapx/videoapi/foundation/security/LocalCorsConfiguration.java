package br.com.fiapx.videoapi.foundation.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.video", name = "storage-mode", havingValue = "mock")
public class LocalCorsConfiguration {
    @Bean
    CorsConfigurationSource corsConfigurationSource(
        @Value("${app.video.local-allowed-origin}") String origin
    ) {
        var api = new CorsConfiguration();
        api.setAllowedOrigins(java.util.List.of(origin));
        api.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "OPTIONS"));
        api.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type", "If-None-Match", "x-amz-checksum-sha256", "X-XSRF-TOKEN"));
        api.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/v1/**", api);
        var upload = new CorsConfiguration(api);
        upload.setAllowCredentials(false);
        source.registerCorsConfiguration("/_local/mock-storage/**", upload);
        return source;
    }
}
