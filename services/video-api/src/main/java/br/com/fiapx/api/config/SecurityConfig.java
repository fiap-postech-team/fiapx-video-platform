package br.com.fiapx.api.config;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.csrf(c -> c.disable()).authorizeHttpRequests(a -> a.requestMatchers("/actuator/health/**", "/actuator/prometheus").permitAll().anyRequest().authenticated()).oauth2ResourceServer(o -> o.jwt(j -> {
        })).build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.security.jwt-secret}") String secret) {
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret.getBytes(), "HmacSHA256")).build();
    }
}
