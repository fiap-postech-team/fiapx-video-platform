package br.com.fiapx.videoapi.identity.application;

import br.com.fiapx.videoapi.identity.application.port.out.IdentityStore;
import br.com.fiapx.videoapi.identity.application.port.out.PasswordHasher;
import br.com.fiapx.videoapi.identity.domain.IdentityUser;
import br.com.fiapx.videoapi.identity.domain.UserRole;
import br.com.fiapx.videoapi.identity.domain.UserStatus;
import java.util.Locale;
import java.util.UUID;

public class RegisterUser {
    private final IdentityStore store;
    private final PasswordHasher passwords;

    public RegisterUser(IdentityStore store, PasswordHasher passwords) {
        this.store = store;
        this.passwords = passwords;
    }

    public IdentityUser execute(String email, String password) {
        var normalizedEmail = normalize(email);
        if (store.findUserForUpdate(normalizedEmail).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }
        var user = new IdentityUser(UUID.randomUUID(), normalizedEmail, passwords.hash(password), UserRole.USER,
            UserStatus.ACTIVE, 0, null);
        return store.saveUser(user);
    }

    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
