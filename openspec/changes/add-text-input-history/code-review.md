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
| 2 | blind | 0 | 1 | 2 | 4 | gpt-5.6-sol:fail claude-fable-5:pass | f8cb9d49 |

Round 2 notes: both counted reviewers attested HEAD
f8cb9d49a32765a1e1980f3c0dbbcbb0af723471 + worktree path. The first
claude-fable-5 dispatch terminated prematurely with NO attestation and no
findings/verdict — INVALID per the attestation rule, excluded from gating,
ledger counts, and the round budget; the reviewer was re-dispatched blind and
the re-dispatch is the counted verdict. Ledger file was excluded from both
reviewers' surface (blind protocol). Both confirmed no gate-manifest touch.
Round-1 finding 3 (tasks.md checkoff contract) was independently adjudicated
conforming by both round-2 reviewers against plan.md step 5 (sol: "align with
plan step 5 bookkeeping"; fable: graded P3 sequencing nit) — dispute resolved,
no baseline edit needed. Trajectory: P0+P1 3→1 (converging, condition b).

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
| 1 | [sol] Icon touch handler consumes ACTION_DOWN in the icon region and opens the picker on any later ACTION_UP with no long-press/slop handling — long-press over the icon can open the picker instead of the stock selection/paste menu (AC history-picker-affordance: "long-press remains the stock menu") | P1 | fixed |
| 2 | [sol] TextInputHistoryMatcher uses locale-sensitive String.toLowerCase(); under Turkish/Azeri locales case-equivalent text (I/i/ı) fails to match, and lowercasing can change string length so highlight indices can refer to the normalized string, not the original candidate (AC history-picker-search-and-selection: case-insensitive matching + highlighting) | P1 | fixed |
| 3 | [sol] Every implementation/test commit also modifies tasks.md although each task's files_allowed excludes it; T4.4's commit touches only tasks.md despite T4.4 allowing only app/** (tasks.md file contracts) | P1 | disputed |
| 4 | [fable] testConsecutiveDuplicateBumpsInsteadOfInserting asserts timestamp >= firstTimestamp — vacuously true even if the bump does not refresh the timestamp; ">" would pin the AC's "refreshed timestamp" clause | P2 | fixed |
| 5 | [fable] Matcher toLowerCase() default-locale hazard (same defect as #2, graded P2 by this reviewer; fix with locale-stable comparison) | P2 | fixed |
| 6 | [fable] Icon OnTouchListener consumes ACTION_UP based solely on UP coordinates: a gesture starting on the text and released over the icon opens the sheet and swallows the UP the EditText needs; should require the DOWN to have landed in-region (same surface as #1, graded P2) | P2 | fixed |
| 7 | [fable] Dismiss-without-pick unconditionally refocuses box + shows IME even when the terminal had focus before the icon tap — beyond-spec focus steal (spec silent) | P3 | deferred |
| 8 | [fable] Fixed 240dp list in a non-weighted LinearLayout can clip the clear-all footer below short windows (landscape + IME) | P3 | deferred |
| 9 | [fable] Greedy leftmost subsequence alignment can under-score candidates where a later alignment yields consecutive runs — ranking imperfection only | P3 | deferred |
| 10 | [R2/sol] TextInputHistory.delete() matches by (text, timestamp); non-consecutive duplicate texts recorded in the same millisecond are indistinguishable — long-press-delete can remove the wrong row (AC history-entry-deletion-and-clearing "that entry is removed") | P1 | fixed |
| 11 | [R2/fable] Unconsumed DOWN arms the EditText's pending long-press check; a tap held just under the timeout can fire the stock menu concurrently with the sheet opening | P2 | fixed |
| 12 | [R2/fable] showSoftInput inside onDismiss can silently no-op before the activity window regains focus — intermittent miss of the IME-return half of the pick AC | P2 | fixed |
| 13 | [R2/fable] tasks.md checked off incrementally per feature commit rather than at plan step 5 — sequencing nit; checkoff pattern itself judged conforming (supersedes finding 3's P1 grading) | P3 | deferred |
| 14 | [R2/fable] History icon invisible to TalkBack (compound drawable, no content description; plan step 3 listed one) — spec silent on a11y | P3 | deferred |

## Applied fixes

- 2e8f37bc716cfb08c9e87279106844ee8c571963 `fix: address code-review round-1 findings (icon gesture, locale-safe fuzzy matching, test pinning)`:
  - Findings 1+6: touch handler no longer consumes DOWN/MOVE anywhere; the picker opens only when DOWN and UP both land in the icon region and the release beats `ViewConfiguration.getLongPressTimeout()` — long-press (over icon or text) and cross-region gestures fall through to stock EditText handling.
  - Findings 2+5: matcher rewritten to per-char locale-independent folding (`Character.toLowerCase`/`toUpperCase`, mirroring `String.regionMatches(ignoreCase)`); no whole-string normalization, so highlight indices always index the original candidate. New regression test runs under a forced `tr_TR` default locale.
  - Finding 4: bump-timestamp assertion tightened to strict `>`.
  - Validation re-run green: `:app:compileDebugJavaWithJavac :app:testDebugUnitTest`, 19 tests, 0 failures.
- 358ce3911c57431c52d90f6dba2a19ae830b1748 `fix: address code-review round-2 findings (identity-based delete, long-press cancel, posted IME return)`:
  - Finding 10 (P1): delete() now matches by object identity — snapshots hand out stored Entry references, so identity uniquely names one row even for same-text same-millisecond duplicates; new unit test pins duplicate disambiguation.
  - Finding 11: `editText.cancelLongPress()` before opening the sheet kills the armed long-press check.
  - Finding 12: focus + `showSoftInput` posted from onDismiss so the activity window regains focus first.
  - Validation re-run green: 20 tests, 0 failures.

## Residual risks

- Finding 3: RESOLVED in round 2 — both blind round-2 reviewers independently judged the worktree-side checkoff pattern conforming to plan.md step 5 (fable regraded the sequencing aspect P3, finding 13). Original orchestrator position retained below for the record.
- Finding 3 (disputed, orchestrator position for round 2): worktree-side tasks.md checkbox flips are the schema's sanctioned progress-tracking mechanism — plan.md step 5 explicitly requires "worktree-side task checkoff", and the gate's tasks check reads those checkboxes from the worktree; task `files_allowed` contracts scope the IMPLEMENTATION surface of each task, not the checkoff bookkeeping that rides every opsx commit. Checkbox-flip diffs are text-otherwise-byte-identical (verified by the second round-1 reviewer, who judged the same pattern conforming). Round 2 blind reviewers adjudicate against the same baseline.
- Findings 7–9 deferred as advisory (P3): spec-silent focus behavior on dismiss-without-pick, short-window footer clipping, and greedy-alignment ranking imperfection — none contract-violating; candidates for a follow-up change.

## Verdict rationale

Round 1 failed on three P1s (gesture handling, locale-sensitive matching, tasks.md
contract — the last resolved as conforming by both round-2 reviewers). Round 2 failed
on one P1 (delete identity ambiguity for same-text same-millisecond duplicates),
graded P3-theoretical by the second reviewer; fixed at 358ce391 with identity-based
deletion plus a pinning test, alongside both round-2 P2s. Trajectory is converging
(P0+P1: 3 → 1) with change-scoped fixes landed after each round — continuation
condition (b); round 3 blind re-dispatch follows against the post-fix HEAD. Verdict
remains fail until a quiet round seals pass.
