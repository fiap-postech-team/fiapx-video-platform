package br.com.fiapx.videoapi.outbox.domain;

import java.time.Instant;
import java.util.UUID;

public record OutboxClaim(UUID eventId, UUID claimToken, String routingKey, String payload,
                          Instant occurredAt) {
}
