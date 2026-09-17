package br.com.fiapx.videoapi.identity.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiapx.videoapi.identity.domain.IdentityUser;
import br.com.fiapx.videoapi.identity.domain.UserRole;
import br.com.fiapx.videoapi.identity.domain.UserStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class JpaIdentityStoreTest {

    @Test
    void initializesPasswordChangeTimestampForNewCredentials() {
        var credentials = mock(UserCredentialRepository.class);
        var user = new IdentityUser(UUID.randomUUID(), "user@example.com", "{bcrypt}hash", UserRole.USER,
            UserStatus.ACTIVE, 0, null);
        when(credentials.findById(user.id())).thenReturn(Optional.empty());

        ReflectionTestUtils.invokeMethod(store(credentials), "saveCredential", user);

        var saved = ArgumentCaptor.forClass(UserCredentialEntity.class);
        verify(credentials).save(saved.capture());
        assertThat(saved.getValue().passwordChangedAt).isNotNull().isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void preservesPasswordChangeTimestampWhenUpdatingAuthenticationState() {
        var credentials = mock(UserCredentialRepository.class);
        var user = new IdentityUser(UUID.randomUUID(), "user@example.com", "{bcrypt}hash", UserRole.USER,
            UserStatus.ACTIVE, 3, null);
        var existing = new UserCredentialEntity();
        var changedAt = Instant.parse("2026-01-01T00:00:00Z");
        existing.userId = user.id();
        existing.passwordChangedAt = changedAt;
        when(credentials.findById(user.id())).thenReturn(Optional.of(existing));

        ReflectionTestUtils.invokeMethod(store(credentials), "saveCredential", user);

        assertThat(existing.passwordChangedAt).isEqualTo(changedAt);
        assertThat(existing.failedAttempts).isEqualTo(3);
    }

    private JpaIdentityStore store(UserCredentialRepository credentials) {
        return new JpaIdentityStore(mock(IdentityUserRepository.class), credentials, mock(UserRoleRepository.class),
            mock(AuthSessionRepository.class), mock(RefreshTokenRepository.class));
    }
}
