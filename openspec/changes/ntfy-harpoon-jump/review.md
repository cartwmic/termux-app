---
# Machine-readable mode block — the SOLE source opsx gate reads (it never parses
# the prose table below). Keep the table in sync as the human-facing mirror.
scale: M
full_rigor: false
execution_mode: standard
verification_mode: retained-recommended
debug_mode: standard
review_status: not-requested
delegation_mode: single-agent
loop_max_iterations: 40
validation_source_mode: required
spec_level: spec-anchored
doneness_mode: required
review_max_rounds: 5
review_budget_mode: quiet-round
review_models: [claude-bridge/claude-opus-4-8, openai-codex/gpt-5.5]
---

# Review

## Modes

| Mode | Value | Notes |
|---|---|---|
| Scale | M | Cross-file, single new capability (notification-driven zellij jump on the phone). |
| full_rigor | false | Single capability, no migration/breaking change/cross-capability spread; design.md decision-gated. |
| Execution Mode | standard | |
| Verification Mode | retained-recommended | Android build is warn-only in gates; verify.md via blind subagent. |
| Debug Mode | standard | |
| Review Status | not-requested | |
| Delegation Mode | single-agent | Orchestrator implements in-session; reviews delegated to blind subagents. |
| Worktree Mode | derived (absent) | M ⇒ worktree-required (blast-radius sandbox for the fork's Android code). |
| Code Review Mode | derived (absent) | M ⇒ gating-required (2-model blind adversarial). |
| Loop Max Iterations | 40 | M default. |
| Validation Source Mode | required | `openspec validate --changes --strict` is the agent-independent source (required:true in opsx-gates.yaml). |
| Doneness Mode | required | Rides the code-review dispatch (plain M, designated reviewer = first review model). |
| Spec Level | spec-anchored | |
| Model Config | review_models pinned | Two distinct models for genuine 2-model blind gating review. |

## Diff Base + Worktree locator

**Diff Base SHA:** 539b50b8c5d713156bded49ebcc8639297306694
**Worktree Path:** /Volumes/Workshop/git/termux-app--opsx-ntfy-harpoon-jump
**Integration Branch:** master

## Manual Adjustments

- Scale M: the termux-app slice adds a new deep-link intent handler + background
  side-channel exec + foreground activity — cross-file (manifest + activity/handler),
  single capability. Not S (multi-file, new behavior), not full_rigor (no
  migration/breaking/cross-capability).
- Integration Branch = master: termux-app's default branch is `master`, not `main`.
- review_models pinned (claude-opus-4-8 + gpt-5.5): OPSX_REVIEW_MODELS unset in the
  environment; pinning two distinct models keeps the M-tier code review a genuine
  2-model blind adversarial dispatch rather than degraded-single-model.

## Execution Notes

- 2026-07-04 — review.md authored; loop start. Scale M / full_rigor false.
- 2026-07-04 — apply: worktree opsx/ntfy-harpoon-jump created, Diff Base 539b50b8. Implemented manifest deep link + ZellijJumpHandler + onNewIntent wiring + unit test. TermuxActivity already launchMode=singleTask (re-delivery requirement pre-satisfied).

## Scope Expansions

- (none yet)
