package br.com.fiapx.videoapi.identity.adapter.in.http;

import br.com.fiapx.videoapi.identity.domain.IdentityUser;
import java.util.List;
import java.util.UUID;

record UserResponse(UUID id, String email, List<String> roles) {
    static UserResponse from(IdentityUser user) {
        return new UserResponse(user.id(), user.email(), List.of(user.role().name()));
    }
}
