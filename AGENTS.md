# Repository Guidelines

## Scope

These instructions apply to the entire FIAPX video platform repository. More
specific `AGENTS.md` files under a service may add or override guidance for that
service.

## Architecture and source of truth

- Preserve the Java 21, Spring Boot, and Maven multi-module architecture defined
  by the parent `pom.xml` and the accepted ADRs.
- Treat `contracts/openapi.yaml` and `contracts/asyncapi.yaml` as the public HTTP
  and messaging contracts. Keep implementations and documentation aligned with
  them.
- Record consequential architecture changes in a new ADR. Never rewrite an
  accepted ADR to hide a decision change; supersede it explicitly.
- Keep service boundaries clear: the API owns job intake and status queries, the
  processor owns media processing, and the notification worker owns delivery of
  user notifications.

## Implementation standards

- Organize production code by business capability. Avoid repository-wide
  technical buckets such as `controller`, `service`, and `repository` when a
  feature package communicates ownership more clearly.
- Prefer constructor injection and immutable dependencies.
- Keep transport, persistence, and messaging details out of domain decisions.
- Do not expose JPA entities as public API contracts. Use request and response
  DTOs at HTTP and messaging boundaries.
- Use UUIDs for aggregate and event identifiers and UTC for timestamps.
- Do not add a production dependency unless the existing platform cannot meet
  the requirement; explain the trade-off when adding one.
- Never commit credentials, tokens, private keys, signed URLs, or production
  connection strings. Configuration must come from environment variables with
  safe local defaults where appropriate.

## Database and messaging

- Change database schemas only through new, forward-only Flyway migrations.
  Never edit a migration that may already have been applied.
- Keep a state change and its transactional outbox record in the same database
  transaction.
- Assume RabbitMQ delivers messages at least once. Consumers must be idempotent
  and safe under redelivery.
- Version externally consumed event schemas and preserve backward compatibility,
  or document and coordinate an intentional breaking change.
- Include stable event identifiers, occurrence timestamps, correlation data,
  event type, and schema version in integration events.

## Testing and verification

- Add or update tests for every behavior change and bug fix.
- Prefer focused unit tests for business rules and use Spring context tests only
  when framework wiring is part of the behavior under test.
- Use Testcontainers for integration behavior that depends on PostgreSQL,
  RabbitMQ, or another real infrastructure contract.
- Run the narrowest relevant module checks while iterating. Before finishing a
  cross-module change, run `./mvnw verify` from the repository root.
- Do not claim a check passed unless it was executed successfully. Report checks
  that could not run and why.

## Repository hygiene

- Do not edit or commit generated output such as `target/`, IDE metadata, logs,
  coverage output, or local environment files.
- Keep changes scoped to the requested behavior and preserve unrelated work in
  the working tree.
- Update the relevant README, contract, and operational documentation whenever
  setup or externally observable behavior changes.

## Code Review Rules

- Flag secrets or sensitive data in source, configuration, logs, fixtures, or
  examples. Use environment-based configuration and sanitized examples instead.
- Flag changes to an existing Flyway migration. Require a new migration.
- Flag message consumers that are not safe under duplicate delivery. Require an
  idempotency key or another durable deduplication strategy.
- Flag state changes followed by direct message publication when failure could
  leave the database and broker inconsistent. Use the transactional outbox.
- Flag HTTP or event contract changes that are not reflected in the matching
  OpenAPI or AsyncAPI document.
- Flag behavior changes without tests covering the changed success and relevant
  failure paths.
