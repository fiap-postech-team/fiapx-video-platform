package br.com.fiapx.videoapi.jobs.application;

public final class UserInactiveException extends RuntimeException {
    public UserInactiveException() {
        super("User is not active");
    }
}
