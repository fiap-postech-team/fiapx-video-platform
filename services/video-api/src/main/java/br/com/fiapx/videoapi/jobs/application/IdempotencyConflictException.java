package br.com.fiapx.videoapi.jobs.application;

/** Raised when an idempotency key is reused for a different job request. */
public final class IdempotencyConflictException extends RuntimeException {
}
