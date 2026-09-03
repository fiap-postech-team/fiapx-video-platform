# Video API Guidelines

## Scope and responsibilities

These instructions apply to `services/video-api` and supplement the repository
guidelines. The Video API authenticates users, accepts processing jobs, persists
job state, publishes work through the transactional outbox, consumes processing
results, and exposes job status. Media processing itself belongs in
`video-processor`.

## HTTP boundary

- Keep controllers limited to HTTP concerns: authentication, validation, DTO
  mapping, status codes, and delegation to an application use case.
- Derive the user identity from the validated JWT. Never trust a client-supplied
  `userId` for ownership or authorization.
- Scope every job lookup and mutation by both job identifier and authenticated
  owner unless an explicitly documented privileged endpoint applies.
- Validate request payloads with Bean Validation and return one consistent error
  shape, preferably Spring `ProblemDetail`.
- Return deliberate status codes and headers. Job creation should preserve the
  asynchronous nature of processing and expose a stable resource location.
- Do not return persistence entities from controllers. Keep external DTOs
  independent from the JPA model.

## Job lifecycle

- Define allowed job-status transitions explicitly and reject invalid or stale
  transitions.
- Persist status history whenever the current job status changes.
- Make processing-result handling idempotent: receiving the same event more than
  once must not duplicate history, regress status, or corrupt the result.
- Treat event ordering as uncertain. Guard against an older event overwriting a
  newer terminal state.
- Keep job creation and its outbox event in one transaction.

## Persistence and outbox

- Keep transaction boundaries in application services, not controllers.
- Avoid unbounded repository reads and N+1 query patterns.
- Publish only unpublished outbox rows and mark a row published only after a
  successful broker operation.
- Design concurrent publishers so that the same outbox row is not intentionally
  dispatched by multiple workers; remain safe if a duplicate still occurs.
- Keep scheduled batch sizes, retry behavior, and polling intervals configurable.

## Security and observability

- Keep health probes accessible only as required by the runtime. Protect other
  actuator endpoints according to the deployment model.
- Do not log JWTs, credentials, signed object-storage URLs, raw personal data, or
  complete event payloads containing sensitive values.
- Propagate correlation identifiers across HTTP requests, persisted jobs, outbox
  events, and processing-result events.
- Emit metrics for job creation, status transitions, outbox backlog and publish
  failures, consumer failures, and processing latency.
- Production startup must fail when required secrets are absent or use an
  insecure placeholder.

## Tests required by change type

- Controller change: cover authentication, ownership, validation, status codes,
  and response contract.
- Job lifecycle change: cover every added transition and invalid transitions.
- Repository or migration change: cover it against PostgreSQL with Testcontainers.
- Publisher or listener change: cover success, retry/redelivery, duplicates, and
  malformed messages with the appropriate Spring AMQP test support.
- Security change: add explicit tests for anonymous, authorized, and cross-user
  access.

## Code Review Rules

- Flag job access performed by `id` alone. The safe path is an owner-scoped query
  using the authenticated JWT subject.
- Flag any request DTO that can select the acting user. Identity must come from
  the security context.
- Flag status assignment that bypasses the lifecycle rules or omits status
  history.
- Flag result listeners that regress terminal states or create duplicate history
  on message redelivery.
- Flag direct RabbitMQ publication during job creation. Persist an outbox record
  in the job transaction and publish it asynchronously.
- Flag an outbox row marked as published before broker success.
- Flag endpoints exposing JPA entities or leaking internal storage keys when a
  safer external representation is available.
- Flag tests that mock PostgreSQL- or RabbitMQ-specific behavior when correctness
  depends on the real integration semantics.
