package br.com.fiapx.videoapi.foundation.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.env.Environment;

@Configuration
@EnableWebSecurity
public class FoundationSecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        Environment environment,
        ProblemDetailAuthenticationEntryPoint authenticationEntryPoint,
        ProblemDetailAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(authorize -> authorizeRequests(authorize, environment))
            .build();
    }

    private void authorizeRequests(
        org.springframework.security.config.annotation.web.configurers
            .AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authorize,
        Environment environment
    ) {
        authorize.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
        if (environment.matchesProfiles("local")) {
            authorize.requestMatchers(
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/openapi.yaml",
                "/v3/api-docs/swagger-config"
            ).permitAll();
        }
        authorize.anyRequest().denyAll();
    }
}
