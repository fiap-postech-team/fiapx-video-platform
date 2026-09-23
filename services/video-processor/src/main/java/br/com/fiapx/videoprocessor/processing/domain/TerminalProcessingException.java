package br.com.fiapx.videoprocessor.processing.domain;

/**
 * Failure that retrying cannot fix, such as unreadable or unsupported media. The {@code reason} is
 * published to consumers and shown to users, so it must never carry a stack trace, a command line,
 * a credential or personal data.
 */
public class TerminalProcessingException extends RuntimeException {

    private final String reason;

    public TerminalProcessingException(String reason) {
        this(reason, null);
    }

    public TerminalProcessingException(String reason, Throwable cause) {
        super(reason, cause);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
