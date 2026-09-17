package br.com.fiapx.videoapi.identity.adapter.in.http;

import br.com.fiapx.videoapi.identity.application.Login;
import br.com.fiapx.videoapi.identity.application.LogoutSession;
import br.com.fiapx.videoapi.identity.application.RefreshSession;
import br.com.fiapx.videoapi.identity.application.RegisterUser;
import br.com.fiapx.videoapi.identity.domain.IdentityUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "FIAPX_REFRESH";
    private final RegisterUser registerUser;
    private final Login login;
    private final RefreshSession refresh;
    private final LogoutSession logout;
    private final CsrfTokenRepository csrf;
    private final AuthCookieFactory cookies;
    private final IdentityTransactions transactions;

    public AuthController(RegisterUser registerUser, Login login, RefreshSession refresh, LogoutSession logout,
                          CsrfTokenRepository csrf, AuthCookieFactory cookies, IdentityTransactions transactions) {
        this.registerUser = registerUser; this.login = login; this.refresh = refresh;
        this.logout = logout; this.csrf = csrf; this.cookies = cookies; this.transactions = transactions;
    }

    @PostMapping("/register")
    ResponseEntity<UserResponse> register(@Valid @RequestBody CredentialsRequest request) {
        return ResponseEntity.status(201).body(UserResponse.from(transactions.execute(() -> registerUser.execute(request.email(), request.password()))));
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody CredentialsRequest request, HttpServletRequest servletRequest,
                                         HttpServletResponse servletResponse) {
        var result = transactions.execute(() -> {
            try {
                return login.execute(request.email(), request.password());
            } catch (br.com.fiapx.videoapi.identity.application.AuthenticationFailedException exception) {
                return null;
            }
        });
        if (result == null) {
            throw new br.com.fiapx.videoapi.identity.application.AuthenticationFailedException();
        }
        add(servletResponse, cookies.refresh(result.refreshToken(), result.refreshExpiresAt()));
        csrf.saveToken(csrf.generateToken(servletRequest), servletRequest, servletResponse);
        return ResponseEntity.ok(LoginResponse.from(result));
    }

    @PostMapping("/refresh")
    ResponseEntity<AccessTokenResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String token,
                                                HttpServletResponse response) {
        var result = transactions.execute(() -> {
            try {
                return refresh.execute(token);
            } catch (br.com.fiapx.videoapi.identity.application.AuthenticationFailedException exception) {
                return null;
            }
        });
        if (result == null) {
            throw new br.com.fiapx.videoapi.identity.application.AuthenticationFailedException();
        }
        add(response, cookies.refresh(result.refreshToken(), result.refreshExpiresAt()));
        return ResponseEntity.ok(AccessTokenResponse.from(result.accessToken()));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String token,
                                HttpServletResponse response) {
        transactions.execute(() -> logout.execute(token));
        add(response, cookies.expiredRefresh());
        add(response, cookies.expiredCsrf());
        return ResponseEntity.noContent().build();
    }

    private void add(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    record CredentialsRequest(@NotBlank @Email String email, @NotBlank @Size(min = 12, max = 128) String password) {
    }

    record UserResponse(java.util.UUID id, String email, List<String> roles) {
        static UserResponse from(IdentityUser user) { return new UserResponse(user.id(), user.email(), List.of(user.role().name())); }
    }

    record LoginResponse(UserResponse user, String accessToken, String tokenType, long expiresIn) {
        static LoginResponse from(Login.LoginResult result) { return new LoginResponse(UserResponse.from(result.user()), result.accessToken().value(), "Bearer", result.accessToken().expiresInSeconds()); }
    }

    record AccessTokenResponse(String accessToken, String tokenType, long expiresIn) {
        static AccessTokenResponse from(br.com.fiapx.videoapi.identity.application.port.out.AccessTokenIssuer.IssuedAccessToken token) { return new AccessTokenResponse(token.value(), "Bearer", token.expiresInSeconds()); }
    }
}
