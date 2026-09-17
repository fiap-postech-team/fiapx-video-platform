package br.com.fiapx.videoapi.identity.adapter.configuration;

import br.com.fiapx.videoapi.identity.application.RegisterUser;
import br.com.fiapx.videoapi.identity.application.port.out.IdentityStore;
import br.com.fiapx.videoapi.identity.application.port.out.PasswordHasher;
import br.com.fiapx.videoapi.identity.domain.IdentityUser;
import br.com.fiapx.videoapi.identity.domain.UserRole;
import br.com.fiapx.videoapi.identity.domain.UserStatus;
import java.util.UUID;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
public class AdminBootstrap {
    @Bean
    ApplicationRunner adminBootstrapRunner(BootstrapProperties properties, IdentityStore store, PasswordHasher passwords,
                                           TransactionTemplate transactions) {
        return arguments -> {
            if (properties.enabled()) {
                transactions.executeWithoutResult(status -> create(properties, store, passwords));
            }
        };
    }

    private void create(BootstrapProperties properties, IdentityStore store, PasswordHasher passwords) {
        validate(properties);
        if (store.hasAdministrator()) {
            return;
        }
        var email = RegisterUser.normalize(properties.email());
        if (store.findUserForUpdate(email).isPresent()) {
            throw new IllegalStateException("Bootstrap administrator email already belongs to a user");
        }
        store.saveUser(new IdentityUser(UUID.randomUUID(), email, passwords.hash(properties.password()),
            UserRole.ADMIN, UserStatus.ACTIVE, 0, null));
    }

    private void validate(BootstrapProperties properties) {
        var password = properties.password();
        if (properties.email() == null || password == null || password.length() < 12 || password.length() > 128) {
            throw new IllegalStateException("Bootstrap administrator configuration is invalid");
        }
    }
}
