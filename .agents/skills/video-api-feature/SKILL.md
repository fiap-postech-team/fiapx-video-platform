---
name: video-api-feature
description: Implement or change a Video API HTTP feature, use case, job behavior, or persistence flow while keeping contracts, security, and tests aligned. Use for feature and bug-fix work inside services/video-api; do not use for changes limited to RabbitMQ event contracts or for review-only requests.
---

# Implement a Video API Feature

Deliver the requested behavior as a cohesive, tested change within
`services/video-api`.

## Establish the change boundary

1. Read the repository and service `AGENTS.md` files.
2. Inspect the relevant feature package, tests, `contracts/openapi.yaml`, and the
   service README before deciding on a design.
3. Identify the authenticated actor, resource ownership rule, allowed job-state
   transitions, transaction boundary, and observable API behavior affected.
4. Preserve the user's requested scope. If the implementation requires a public
   breaking change or a new production dependency, explain the need before
   expanding the work.

## Implement

- Keep HTTP DTOs and JPA entities separate.
- Derive identity and authorization from the security context rather than request
  data.
- Put business decisions and transaction boundaries in the application layer;
  keep controllers focused on HTTP concerns.
- Model lifecycle changes explicitly. Persist status history with the current
  state update.
- When a state change emits work, persist its outbox event in the same
  transaction. Do not publish directly from the request transaction.
- Add a new Flyway migration for schema changes; never edit an applied migration.
- Update OpenAPI and the service README when externally observable behavior or
  setup changes.

## Verify

- Add focused tests for the behavior first or alongside the implementation.
- Cover success, validation, authentication, ownership, invalid transitions, and
  relevant not-found or conflict paths.
- Use PostgreSQL Testcontainers where database semantics matter.
- Run the Video API module tests while iterating. Run the repository verification
  command when the change affects shared contracts or other modules.
- Review the final diff for leaked internals, secrets, unrelated changes, and
  documentation drift. Report the exact checks run and any that could not run.
