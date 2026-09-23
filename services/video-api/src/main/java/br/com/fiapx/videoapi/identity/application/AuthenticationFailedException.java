package br.com.fiapx.videoapi.identity.application;

public final class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException() {
        super("Authentication failed");
    }
}
