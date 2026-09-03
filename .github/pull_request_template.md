## Context

<!-- Why is this change needed? Link an existing issue or ADR when applicable. -->

## Changes

<!-- Summarize the reviewer-relevant implementation and behavior changes. -->

- Describe the primary change.

## Validation

<!-- List commands and meaningful manual checks. Do not mark checks that were not run. -->

- [ ] `git diff --check`
- [ ] `./mvnw verify`
- [ ] `docker compose config --quiet`
- [ ] Additional affected-module or contract checks described below

Validation details:

<!-- Include relevant scenarios, test scope, and any check that could not run. -->

## Impact and risk

- [ ] No public HTTP contract change
- [ ] OpenAPI updated for an HTTP contract change
- [ ] No integration-event contract change
- [ ] AsyncAPI and event catalog updated for an event change
- [ ] No database schema change
- [ ] New forward-only Flyway migration included
- [ ] No new production dependency
- [ ] Security and cross-user access impact reviewed

<!-- Explain relevant compatibility, migration, deployment, rollback, or operational risk. -->

## Reviewer notes

<!-- Highlight the most important files, decisions, trade-offs, or follow-up work. -->

## Checklist

- [ ] The branch is updated with the current target branch
- [ ] The change is focused and excludes generated or unrelated files
- [ ] Tests cover the changed success and relevant failure paths
- [ ] Documentation and contracts match externally observable behavior
- [ ] No secrets or sensitive data were added to source, fixtures, logs, or examples
