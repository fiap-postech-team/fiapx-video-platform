package br.com.fiapx.videoapi.jobs.application.port.out;

import java.util.UUID;

@FunctionalInterface
public interface UuidGenerator {
    UUID next();
}
