# Code Review

**Change:** add-text-input-history
**Verdict:** fail
**review_mode:** adversarial-multimodel
**reviewer-provenance:** subagent reviewer (openai-codex/gpt-5.6-sol); subagent reviewer (claude-bridge/claude-fable-5) — parallel blind dispatch, cwd = worktree
**Diff Base SHA:** 70a67e65e8ec7eb2b6685bdde2e5cdc1789c18e7
**Reviewed Range:** 70a67e65e8ec7eb2b6685bdde2e5cdc1789c18e7..46af3bbb90c986ee4e3e3c346dd1f26e0ddffc45
**Attested HEAD:** 46af3bbb90c986ee4e3e3c346dd1f26e0ddffc45
**Baseline:** intent.md (554c80c5) + proposal + specs/terminal-toolbar delta + plan + tasks (all committed, integration branch master); design.md absent (decision-gated skip recorded in proposal)
**Generated:** 2026-07-11

## Round tracker

| Round | Mode | P0 | P1 | P2 | P3 | Reviewer verdicts | Reviewed HEAD |
|---|---|---|---|---|---|---|---|
| 1 | blind | 0 | 3 | 3 | 3 | gpt-5.6-sol:fail claude-fable-5:pass | 46af3bbb |

Round 1 notes: both reviewers attested HEAD 46af3bbb90c986ee4e3e3c346dd1f26e0ddffc45
and the worktree path (valid, counted). The claude-fable-5 run ended with a harness
warning ("claude-p stdout closed before a terminal result line; McpNotReady") after
emitting a complete attested review — output counted; warning recorded here for
provenance honesty. Consolidated counts are MAX across reviewers per severity (no
cross-reviewer finding matching). Gate-manifest check: both reviewers independently
confirmed the diff does NOT touch openspec/opsx-gates.yaml or any gate/validation
manifest.

## Findings

| # | Finding | Severity | Status |
|---|---|---|---|
| 1 | [sol] Icon touch handler consumes ACTION_DOWN in the icon region and opens the picker on any later ACTION_UP with no long-press/slop handling — long-press over the icon can open the picker instead of the stock selection/paste menu (AC history-picker-affordance: "long-press remains the stock menu") | P1 | open |
| 2 | [sol] TextInputHistoryMatcher uses locale-sensitive String.toLowerCase(); under Turkish/Azeri locales case-equivalent text (I/i/ı) fails to match, and lowercasing can change string length so highlight indices can refer to the normalized string, not the original candidate (AC history-picker-search-and-selection: case-insensitive matching + highlighting) | P1 | open |
| 3 | [sol] Every implementation/test commit also modifies tasks.md although each task's files_allowed excludes it; T4.4's commit touches only tasks.md despite T4.4 allowing only app/** (tasks.md file contracts) | P1 | open |
| 4 | [fable] testConsecutiveDuplicateBumpsInsteadOfInserting asserts timestamp >= firstTimestamp — vacuously true even if the bump does not refresh the timestamp; ">" would pin the AC's "refreshed timestamp" clause | P2 | open |
| 5 | [fable] Matcher toLowerCase() default-locale hazard (same defect as #2, graded P2 by this reviewer; fix with locale-stable comparison) | P2 | open |
| 6 | [fable] Icon OnTouchListener consumes ACTION_UP based solely on UP coordinates: a gesture starting on the text and released over the icon opens the sheet and swallows the UP the EditText needs; should require the DOWN to have landed in-region (same surface as #1, graded P2) | P2 | open |
| 7 | [fable] Dismiss-without-pick unconditionally refocuses box + shows IME even when the terminal had focus before the icon tap — beyond-spec focus steal (spec silent) | P3 | open |
| 8 | [fable] Fixed 240dp list in a non-weighted LinearLayout can clip the clear-all footer below short windows (landscape + IME) | P3 | open |
| 9 | [fable] Greedy leftmost subsequence alignment can under-score candidates where a later alignment yields consecutive runs — ranking imperfection only | P3 | open |

## Applied fixes

- (none yet — round 1 sealed fail; fix round follows)

## Residual risks

- (to be filled when findings are resolved or deferred)

## Verdict rationale

Round 1 splits: gpt-5.6-sol fails on three P1s (long-press-over-icon gesture handling,
locale-sensitive fuzzy matching, tasks.md file-contract breach); claude-fable-5 passes
with the first two graded P2 and the tasks.md checkoff pattern judged conforming
(worktree-side checkoff riding each commit, per plan.md step 5). Under the
max-across-reviewers rule the round carries open P1s, so the consolidated verdict is
fail. Continuation condition (b) applies: land change-scoped fixes for findings 1/2
(and the overlapping 4–6), address 3, then re-dispatch a fresh blind round.
