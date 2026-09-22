package br.com.fiapx.videoapi.foundation.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.cors.CorsConfigurationSource;
import br.com.fiapx.videoapi.identity.adapter.in.security.LocalJwtAuthenticationConverter;

/**
 * Defines the deny-by-default HTTP policy for the unauthenticated foundation stage.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public final class FoundationSecurityConfiguration {

    /**
     * Creates the stateless security configuration.
     */
    public FoundationSecurityConfiguration() {
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        Environment environment,
        ProblemDetailAuthenticationEntryPoint authenticationEntryPoint,
        ProblemDetailAccessDeniedHandler accessDeniedHandler,
        CsrfTokenRepository csrfTokenRepository,
        CorsConfigurationSource corsConfigurationSource,
        ObjectProvider<JwtDecoder> decoder,
        ObjectProvider<LocalJwtAuthenticationConverter> converter
    ) throws Exception {
        var requestMatcherBuilder = PathPatternRequestMatcher.withDefaults();
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .requireCsrfProtectionMatcher(new OrRequestMatcher(
                    requestMatcherBuilder.matcher(HttpMethod.POST, "/v1/auth/refresh"),
                    requestMatcherBuilder.matcher(HttpMethod.POST, "/v1/auth/logout"))))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(authorize -> authorizeRequests(authorize, environment));
        configureResourceServer(http, decoder.getIfAvailable(), converter.getIfAvailable());
        return http.build();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository(Environment environment) {
        var repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> cookie.sameSite("Strict").secure(!environment.matchesProfiles("local")));
        return repository;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(Environment environment) {
        if (environment.matchesProfiles("local")
            && "mock".equalsIgnoreCase(environment.getProperty("app.video.storage-mode"))) {
            return WebCors.localMockSource(environment.getProperty("app.video.local-allowed-origin"));
        }
        return WebCors.source(environment.getProperty("app.web.allowed-origins", ""));
    }

    private void configureResourceServer(HttpSecurity http, JwtDecoder decoder,
                                         LocalJwtAuthenticationConverter converter) throws Exception {
        if (decoder == null || converter == null) {
            return;
        }
        http.oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.decoder(decoder)
            .jwtAuthenticationConverter(converter)));
    }

    private void authorizeRequests(
        org.springframework.security.config.annotation.web.configurers
            .AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authorize,
        Environment environment
    ) {
        authorize.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
        authorize.requestMatchers("/actuator/prometheus").permitAll();
        authorize.requestMatchers("/v1/auth/register", "/v1/auth/login", "/v1/auth/refresh", "/v1/auth/logout").permitAll();
        if (environment.matchesProfiles("local")) {
            authorize.requestMatchers(
                "/_local/mock-storage/uploads/**",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/openapi.yaml",
                "/v3/api-docs/swagger-config"
            ).permitAll();
        }
        authorize.anyRequest().authenticated();
    }
}
