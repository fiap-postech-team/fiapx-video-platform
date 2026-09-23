package br.com.fiapx.videoapi.identity.application;

import br.com.fiapx.videoapi.identity.application.port.out.IdentityStore;
import br.com.fiapx.videoapi.identity.domain.IdentityUser;
import java.util.UUID;

public class GetCurrentUser {
    private final IdentityStore store;

    public GetCurrentUser(IdentityStore store) {
        this.store = store;
    }

    public IdentityUser execute(UUID userId) {
        return store.findUser(userId).orElseThrow(AuthenticationFailedException::new);
    }
}
