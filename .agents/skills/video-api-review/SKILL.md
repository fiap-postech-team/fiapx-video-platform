---
name: video-api-review
description: Review Video API code or a proposed diff for correctness, security, contract compatibility, data consistency, messaging reliability, and missing tests. Use only for review, audit, or risk-assessment requests; do not implement fixes unless explicitly requested.
---

# Review the Video API

Produce an evidence-based review prioritized by user impact. Treat findings as
actionable defects, not general style preferences.

## Establish the baseline

1. Read the applicable `AGENTS.md` files and inspect the requested diff or target
   files.
2. Read the relevant OpenAPI or AsyncAPI sections, ADRs, migrations, and tests
   needed to determine intended behavior.
3. Trace changed behavior end to end: authenticated request, authorization,
   transaction, persistence, outbox, broker, consumer, and response or status
   observation. Follow only paths affected by the review scope.

## Review priorities

Check for:

- cross-user job access, identity accepted from request data, permissive endpoint
  rules, insecure secret defaults, or sensitive logging;
- invalid or regressive job-state transitions, missing history, lost updates, and
  unsafe concurrent handling;
- database writes and event creation outside one transaction;
- premature outbox publication markers, duplicate dispatch hazards, non-idempotent
  consumers, unbounded retries, and failures silently acknowledged;
- HTTP or event behavior drifting from OpenAPI, AsyncAPI, or documented service
  responsibilities;
- edited historical Flyway migrations, unsafe schema evolution, unbounded reads,
  and PostgreSQL behavior hidden by unrealistic mocks;
- missing tests for security boundaries, failure paths, redelivery, duplicates,
  and integration semantics.

## Report findings

- Order findings by severity and likelihood. Lead with findings; keep summaries
  secondary.
- For each finding, identify the smallest relevant file and line range, the
  concrete failure scenario, its impact, and the safe correction direction.
- Do not report formatting preferences or speculative concerns without a
  reproducible path from the reviewed code.
- Distinguish confirmed defects from questions or assumptions.
- If no actionable findings remain, say so and mention meaningful residual test
  or observability gaps.
- Do not modify code, contracts, or external systems during a review unless the
  user explicitly asks for fixes.
