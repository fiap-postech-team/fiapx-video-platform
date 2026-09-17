package br.com.fiapx.videoapi.identity.adapter.configuration;

import br.com.fiapx.videoapi.identity.application.port.out.IdentityStore;
import br.com.fiapx.videoapi.identity.application.port.out.PasswordHasher;
import br.com.fiapx.videoapi.identity.domain.IdentityUser;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityConfigurationSupportTest {
    @Test
    void audienceValidatorAcceptsOnlyExpectedAudience() {
        var validator = new AudienceValidator("video-api");
        assertThat(validator.validate(jwt("video-api")).hasErrors()).isFalse();
        assertThat(validator.validate(jwt("other")).hasErrors()).isTrue();
    }

    @Test
    void bootstrapIsDisabledOrIdempotentAndCreatesOnlyConfiguredAdministrator() throws Exception {
        var bootstrap = new AdminBootstrap();
        var store = mock(IdentityStore.class);
        var passwords = mock(PasswordHasher.class);
        var transactions = transactionTemplate();
        var disabled = bootstrap.adminBootstrapRunner(new BootstrapProperties(false, null, null), store, passwords, transactions);
        disabled.run(null);
        verify(store, never()).saveUser(any());

        when(store.hasAdministrator()).thenReturn(false);
        when(store.findUserForUpdate("admin@example.test")).thenReturn(Optional.empty());
        when(passwords.hash("password-which-is-long-enough")).thenReturn("hashed");
        bootstrap.adminBootstrapRunner(new BootstrapProperties(true, " Admin@Example.Test ", "password-which-is-long-enough"), store, passwords, transactions).run(null);
        verify(store).saveUser(any(IdentityUser.class));

        when(store.hasAdministrator()).thenReturn(true);
        bootstrap.adminBootstrapRunner(new BootstrapProperties(true, "admin@example.test", "password-which-is-long-enough"), store, passwords, transactions).run(null);
    }

    @Test
    void bootstrapRejectsPartialConfigurationAndUserCollision() throws Exception {
        var bootstrap = new AdminBootstrap();
        var store = mock(IdentityStore.class);
        var transactions = transactionTemplate();
        var passwords = mock(PasswordHasher.class);
        when(store.hasAdministrator()).thenReturn(false);
        assertThatThrownBy(() -> bootstrap.adminBootstrapRunner(new BootstrapProperties(true, "admin@example.test", "short"), store, passwords, transactions).run(null))
            .isInstanceOf(IllegalStateException.class);

        when(store.findUserForUpdate("admin@example.test")).thenReturn(Optional.of(mock(IdentityUser.class)));
        assertThatThrownBy(() -> bootstrap.adminBootstrapRunner(new BootstrapProperties(true, "admin@example.test", "password-which-is-long-enough"), store, passwords, transactions).run(null))
            .isInstanceOf(IllegalStateException.class);
    }

    private TransactionTemplate transactionTemplate() {
        var transactions = mock(TransactionTemplate.class);
        doAnswer(invocation -> {
            invocation.<java.util.function.Consumer<org.springframework.transaction.TransactionStatus>>getArgument(0)
                .accept(mock(org.springframework.transaction.TransactionStatus.class));
            return null;
        }).when(transactions).executeWithoutResult(any());
        return transactions;
    }

    private Jwt jwt(String audience) {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject("subject").audience(java.util.List.of(audience))
            .issuedAt(Instant.EPOCH).expiresAt(Instant.EPOCH.plusSeconds(60)).build();
    }
}
