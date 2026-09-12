---
name: open-pull-request
description: Prepare, open, and validate a pull request for this repository, including base-branch synchronization, focused verification, standardized title and description, CI monitoring, and merge-readiness reporting. Use when asked to create, update, or check a PR; do not merge unless the user explicitly requests the merge.
---

# Open and Validate a Pull Request

Create a reviewable PR whose branch is current, checks are evidenced, and risks
are visible to reviewers.

## Prepare the branch

1. Read the applicable `AGENTS.md` files and inspect the complete working tree.
2. Confirm the current head branch and intended base branch. Use `develop` unless
   the user or repository context specifies another base.
3. Preserve unrelated and untracked user files. Never include generated output,
   IDE metadata, secrets, or local runtime data.
4. Fetch the remote base and inspect divergence before integrating it. On an
   already published collaborative branch, prefer merging the current remote
   base unless the user explicitly asks for a rebase or the repository mandates
   one.
5. Resolve conflicts according to the intended behavior of both branches. Do not
   choose a side mechanically; inspect the base change and retain compatible
   requirements from each side.
6. Review the final diff and commit only changes belonging to the PR.

## Verify before publishing

- Run `git diff --check`.
- Run focused tests for changed modules while iterating.
- Before publishing, run `./mvnw verify` for Java changes or cross-module changes.
- Run `docker compose config --quiet` when Compose configuration exists or was
  affected.
- Validate any changed OpenAPI or AsyncAPI contract with an available project
  tool. If no validator is configured, report that limitation.
- Never state that a check passed unless its command completed successfully.

## Compose the PR

- Follow Conventional Commits style for the title:
  `<type>(optional-scope): imperative summary`.
- Keep the title specific and preferably under 72 characters.
- Use `.github/pull_request_template.md` as the description structure. Replace
  guidance and placeholders with concrete information; do not leave empty
  boilerplate sections without explanation.
- Explain why the change is needed, what changed, how it was validated, and any
  contracts, migrations, security, deployment, or rollback impact.
- Link relevant issues or ADRs when they exist. Do not invent issue references.
- Mark unfinished or intentionally blocked work as a draft PR.

## Publish and monitor

1. Push the head branch without force unless the user explicitly approved
   history rewriting.
2. Prefer the GitHub CLI for creating and inspecting the PR. Use an authenticated
   browser session only when the CLI is unavailable or not authenticated.
3. Verify that the PR base and head are correct immediately after creation.
4. Wait for required CI checks to reach a terminal state. Report failures with
   the failing job and useful error context; fix them only when the user's request
   includes remediation.
5. Report merge readiness using GitHub's authoritative state: conflicts,
   required checks, required approvals, conversations, and branch protection.
6. A merge-ready PR is not authorization to merge. Merge only after a separate,
   explicit user request.

## Final report

Return the PR link, base and head branches, checks executed locally, CI results,
and any remaining blocker to merge. Distinguish “CI passed” from “merge-ready”
when approvals or repository policies remain outstanding.
