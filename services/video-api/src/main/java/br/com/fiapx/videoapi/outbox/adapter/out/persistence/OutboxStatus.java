package br.com.fiapx.videoapi.outbox.adapter.out.persistence;

enum OutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED
}
