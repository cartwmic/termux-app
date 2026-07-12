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
| 3 | blind | 0 | 1 | 1 | 5 | gpt-5.6-sol:fail claude-fable-5:pass | 1ed24223 |

Round 3 notes: both reviewers attested HEAD
1ed24223a43b63bcf8e4052653e9f741fdad0b16 + worktree path (both valid, counted;
no dispatch anomalies). Ledger excluded from both surfaces. Both confirmed no
gate-manifest touch; both independently re-confirmed the tasks.md checkoff
pattern conforms to plan.md step 5. Trajectory: P0+P1 3→1→1 — fixes landed
after each round (condition b, converging; each round's P1 is a NEW finding,
not a persisting one: round-2's delete-identity fix was verified by both
round-3 reviewers). Round 4 is within review_max_rounds (5).

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
| 15 | [R3/sol] Matcher folds case per UTF-16 char; supplementary-plane case pairs (Deseret 𐐀 U+10400 / 𐐨 U+10428) fail to match — code-point iteration required (AC history-picker-search-and-selection: case-insensitive matching) | P1 | fixed |
| 16 | [R3/fable] Fixed 240dp list + footer in wrap_content LinearLayout can clip below IME on short screens/landscape (re-statement of finding 8) | P2 | deferred |
| 17 | [R3/fable] cancelLongPress() clears the long-press check but not the lingering prepressed/pressed visual state after a consumed icon tap | P3 | deferred |
| 18 | [R3/fable] Navigator cycle snapshot can recall just-deleted texts if entries are deleted via the sheet mid-cycle (fixed-snapshot readline semantics; baseline silent) | P3 | deferred |
| 19 | [R3/fable] No touch-slop tracking: DOWN on icon → drag away → return → UP still opens the sheet (baseline silent) | P3 | deferred |

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
- ed433a37bda16a095de9a6ae80bd1b09d82c3388 `fix: fold case per Unicode code point in fuzzy matcher (code-review round-3 finding)`:
  - Finding 15 (P1): matcher iterates Unicode code points (query and candidate); case comparison via Character.toLowerCase(int)/toUpperCase(int); consecutive-run bonus advances by charCount; matchedIndices remain UTF-16 start offsets into the ORIGINAL candidate; sheet highlight spans the full charCount so supplementary characters bold whole. New test pins Deseret U+10400↔U+10428 folding (forced full rerun: 21 tests, 0 failures).

## Residual risks

- Finding 3: RESOLVED in round 2 — both blind round-2 reviewers independently judged the worktree-side checkoff pattern conforming to plan.md step 5 (fable regraded the sequencing aspect P3, finding 13). Original orchestrator position retained below for the record.
- Finding 3 (disputed, orchestrator position for round 2): worktree-side tasks.md checkbox flips are the schema's sanctioned progress-tracking mechanism — plan.md step 5 explicitly requires "worktree-side task checkoff", and the gate's tasks check reads those checkboxes from the worktree; task `files_allowed` contracts scope the IMPLEMENTATION surface of each task, not the checkoff bookkeeping that rides every opsx commit. Checkbox-flip diffs are text-otherwise-byte-identical (verified by the second round-1 reviewer, who judged the same pattern conforming). Round 2 blind reviewers adjudicate against the same baseline.
- Findings 7–9, 13–14, 16–19 deferred as advisory (P2/P3, none contract-violating): spec-silent focus behavior on dismiss-without-pick, short-window footer clipping (8/16), checkoff sequencing nit, TalkBack reachability of the icon, pressed-state cosmetic linger, mid-cycle deletion staleness under fixed-snapshot readline semantics, and touch-slop nit — candidates for a follow-up change (a11y being the most user-visible).

## Verdict rationale

Round 1 failed on three P1s (gesture handling, locale-sensitive matching, tasks.md
contract — the last resolved as conforming by both round-2 reviewers). Round 2 failed
on one new P1 (delete identity ambiguity), fixed at 358ce391 with a pinning test.
Round 3 failed on one new P1 (supplementary-plane case folding), fixed at ed433a37
with a pinning test; both round-3 reviewers verified all prior fixes hold. Each
round's P1 is a fresh finding — nothing persists across rounds — and change-scoped
fixes landed after every round: continuation condition (b), round 4 blind re-dispatch
follows against the post-fix HEAD within the 5-round cap. Verdict remains fail until
a quiet round seals pass.
