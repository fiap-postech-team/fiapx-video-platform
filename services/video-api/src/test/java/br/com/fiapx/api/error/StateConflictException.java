package br.com.fiapx.api.error;

/** Raised when an operation is incompatible with the current state of the resource (HTTP 409). */
public class StateConflictException extends RuntimeException {
    public StateConflictException(String message) {
        super(message);
    }
}
