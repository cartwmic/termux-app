# Code Review

**Change:** ntfy-harpoon-jump
**Verdict:** pass
**review_mode:** adversarial-multimodel
**reviewer-provenance:** pi-subagents delegate adapter — blind reviewers claude-bridge/claude-opus-4-8 + openai-codex/gpt-5.5 (2 rounds)
**Diff Base SHA:** 539b50b8c5d713156bded49ebcc8639297306694
**Reviewed Range:** 539b50b8c5d713156bded49ebcc8639297306694..3468c6367f5ff839601cededcd773c129d0d1a56
**Baseline:** intent.md + proposal + specs + design + plan + tasks status
**Generated:** 2026-07-04

## Round tracker

| Round | Mode | P0 | P1 | P2 | P3 | Reviewer verdicts | Reviewed HEAD |
|---|---|---|---|---|---|---|---|
| 1 | blind | 0 | 1 | 2 | 1 | opus-4-8:pass gpt-5.5:fail | 3457e054 |
| 2 | blind | 0 | 0 | 1 | 2 | opus-4-8:pass gpt-5.5:pass | 3468c636 |

Consolidated counts = MAX across reviewers per severity (no cross-reviewer
matching). Round 2 is a quiet round (P0+P1 = 0) → sealed pass.

## Findings

| # | Finding | Severity | Status |
|---|---|---|---|
| 1 | Jump dispatched via `RunCommandService`, which enforces the `allow-external-apps` policy on the in-app caller → jump silently no-ops by default (violates spec `notification-jump.notification-jump-deep-link` / `background-single-session-reuse`) | P1 | fixed |
| 2 | Exported VIEW filter lets any app fire `termux://zellij-jump/<x>` with an unsanitized pane id passed verbatim to the user-space script; safe at fork boundary (String[] argv, no shell) — residual belongs to the out-of-scope jump script | P2 | deferred |
| 3 | `onCreate` re-dispatches the jump on activity recreation (no `savedInstanceState==null` guard / intent not consumed); harmless, idempotent re-focus | P3 | deferred |
| 4 | Only `extractPaneId` unit-tested; `handle()` graceful paths need an Android `Context` (matches tasks 3.1 pure-logic scope) | P3 | deferred |

## Applied fixes

- Finding #1 (P1): commit `3468c636` — dispatch `TermuxService.ACTION_SERVICE_EXECUTE`
  directly (internal execution path, same as `FileReceiverActivity`), bypassing
  `RunCommandService`'s allow-external-apps gate; still background
  (`Runner.APP_SHELL`, no new visible session). Both round-2 reviewers confirmed
  the command now executes in-app by default.

## Residual risks

- P2/P3 advisories routed to `follow-ups.md` (out-of-scope hardening for the
  successor chezmoi/remote-script slice and optional recreation-guard). None
  contract-violating.

## Verdict rationale

Round 1 surfaced one genuine correctness defect (P1 #1: the jump no-oped by
default behind the allow-external-apps policy) — a split verdict (opus advisory,
gpt gating; consolidated max = P1). The fix landed in `3468c636`; round 2 ran a
full blind re-review at the new HEAD and both distinct models returned pass with
0 P0/P1. Quiet round → sealed pass. Two models participated blind
(adversarial-multimodel).
