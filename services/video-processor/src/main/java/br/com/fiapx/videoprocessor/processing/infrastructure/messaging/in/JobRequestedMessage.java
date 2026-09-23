package br.com.fiapx.videoprocessor.processing.infrastructure.messaging.in;

import java.util.UUID;

/**
 * Wire shape of {@code video.job.requested.v1}. Optional envelope fields the producer may add are
 * ignored by the converter, so only what this worker needs is declared here.
 */
public record JobRequestedMessage(UUID eventId, UUID jobId, UUID userId, String sourceKey, UUID correlationId) {}
