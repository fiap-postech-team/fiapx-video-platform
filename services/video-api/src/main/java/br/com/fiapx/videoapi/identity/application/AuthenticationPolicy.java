package br.com.fiapx.videoapi.identity.application;

import java.time.Duration;

public record AuthenticationPolicy(Duration lockDuration, Duration refreshDuration, int maxFailures,
                                   String dummyHash) {
}
