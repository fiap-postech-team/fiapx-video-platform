package br.com.fiapx.videoapi.identity.adapter.in.http;

import br.com.fiapx.videoapi.identity.application.GetCurrentUser;
import br.com.fiapx.videoapi.identity.domain.AuthenticatedIdentity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/me")
public class CurrentUserController {
    private final GetCurrentUser getCurrentUser;

    public CurrentUserController(GetCurrentUser getCurrentUser) {
        this.getCurrentUser = getCurrentUser;
    }

    @GetMapping
    ResponseEntity<UserResponse> get(@AuthenticationPrincipal AuthenticatedIdentity identity) {
        return ResponseEntity.ok(UserResponse.from(getCurrentUser.execute(identity.userId())));
    }
}
