---
name: video-api-event-change
description: Design or implement RabbitMQ event and transactional-outbox changes involving the Video API, including producer-consumer compatibility, idempotency, retries, and AsyncAPI. Use when a task changes an event payload, routing, publication, or consumption; do not use for HTTP-only features.
---

# Change a Video API Event

Keep the asynchronous contract compatible and the database-to-broker workflow
recoverable under partial failure and redelivery.

## Map the event flow

1. Read both applicable `AGENTS.md` files, `contracts/asyncapi.yaml`, the event
   catalog, and ADRs covering RabbitMQ, outbox, and at-least-once delivery.
2. Locate every producer and consumer of the affected event across the
   repository. Include routing configuration, persistence, tests, and operational
   documentation.
3. State whether the change is backward compatible. If it is breaking, introduce
   an explicit new event/schema version and keep coexistence possible unless the
   user has approved a coordinated cutover.

## Preserve delivery invariants

- Give each event a stable event ID, event type, schema version, occurrence time,
  correlation ID, and payload containing only data consumers require.
- Persist domain state and the outbox record atomically.
- Mark an outbox record published only after successful broker publication.
- Make consumers idempotent using a durable event identity or an equivalent
  invariant. Never rely on in-memory deduplication for correctness.
- Treat messages as potentially duplicated, delayed, and out of order. Prevent
  stale results from regressing terminal job states.
- Define the disposition of transient failures, permanent failures, and malformed
  messages. Keep retries bounded and route exhausted or unrecoverable messages to
  the established dead-letter path when one exists.
- Do not place secrets, signed URLs, or unnecessary personal data in events or
  logs.

## Keep contracts synchronized

- Update AsyncAPI, the event catalog, configuration, and all affected producer
  and consumer code together.
- Add or adjust migrations when durable idempotency, versioning, or outbox state
  needs new storage.
- Add tests for serialization, routing, successful handling, duplicate delivery,
  out-of-order delivery where relevant, retries, and malformed input.
- Use real RabbitMQ/PostgreSQL integration semantics when mocks would hide the
  behavior being changed.
- Run affected module tests and then cross-module verification. Summarize the
  compatibility decision and exact checks performed.
