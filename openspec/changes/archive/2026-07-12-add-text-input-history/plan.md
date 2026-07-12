<!-- authored: in-session -->

# Execution Plan

Execution Mode is `standard` (not tdd-required), so plan steps use simple ordered actions; tests land in step 4 before validation. All implementation happens in the `opsx/add-text-input-history` worktree; the locator in review.md is captured at worktree creation.

## Plan step 1: History store and fuzzy matcher

- **Covers:** T1.1, T1.2
- **Pre-conditions:**
  - Worktree `opsx/add-text-input-history` created; Diff Base SHA + Worktree Path captured into review.md (integration checkout).
- **Action:**
  1. Create `TextInputHistory` (new file, `com.termux.app.terminal.io`): static process-lifetime singleton; `ArrayList` of immutable `Entry(text, epochMillis)` most-recent-first; `record` (skip empty; equal-to-head → move/refresh; insert at head; trim >100 from tail), `delete`, `clear`, `snapshot` (defensive copy). No android.* dependencies beyond nothing — pure Java for testability. No persistence of any kind.
  2. Create `TextInputHistoryMatcher` (new file): static `match(query, candidate)` → nullable result of (score, matched indices); case-insensitive subsequence; score rewards consecutive runs and early first-match; empty query matches everything with zero score.
  3. Commit on the worktree branch (`feat: add in-memory text-input history store and fuzzy matcher`).
- **Verification:** `./gradlew :app:compileDebugJavaWithJavac` compiles.
- **Rollback:** revert the commit; files are new, no callers yet.

## Plan step 2: Capture hook and hardware-keyboard cycling

- **Covers:** T2.1, T2.2
- **Pre-conditions:** step 1 merged into the worktree branch history.
- **Action:**
  1. In `TerminalToolbarViewPager.setupTextInputView`, before `editText.setText("")` in the send path: `if (textToSend.length() > 0 or original text non-empty) TextInputHistory.record(<original box text>)` — record only the user-typed text (never the `\r` substitute), only when the session existed and the send proceeded; finished-session removal path records nothing.
  2. Create `TextInputHistoryNavigator` (new file): pure-Java cursor over a history snapshot holding (position, saved draft); `up()`/`down()` return the text to show, `onUserEdit()` resets. Wire an `OnKeyListener` in `setupTextInputView`: `KEYCODE_DPAD_UP`/`DOWN` on ACTION_DOWN while box focused → navigator drives `setText` + cursor-to-end; return false otherwise so all other keys keep stock behavior; a TextWatcher distinguishes programmatic sets from user edits for cycle reset; empty history → inert (return false).
  3. Commit (`feat: capture toolbar text-input history and add hw-keyboard cycling`).
- **Verification:** compile green; manual reasoning check against domain invariants 1–4 (send semantics untouched — capture reads, never mutates the write).
- **Rollback:** revert commit; hook is additive lines in one method plus a new file.

## Plan step 3: History icon and bottom-sheet picker

- **Covers:** T3.1, T3.2
- **Pre-conditions:** steps 1–2 done.
- **Action:**
  1. Add `ic_history.xml` vector drawable (white, 24dp) and set it via `setCompoundDrawablesRelativeWithIntrinsicBounds` end-slot (or `android:drawableEnd`) on the text-input EditText; `OnTouchListener` detects taps within the end-drawable region and opens the sheet, returning false elsewhere so cursor placement, focus, and long-press selection behavior are untouched; wired inside `setupTextInputView` so both toolbar modes get it.
  2. Create `view_text_input_history_sheet.xml` (search `EditText` + `RecyclerView` + clear-all footer + empty/no-match hint `TextView`) and `item_text_input_history_entry.xml` (entry text with `SpannableString` bold-highlight of matched indices; dim relative timestamp via `DateUtils.getRelativeTimeSpanString`).
  3. Create `TextInputHistorySheet` wrapping `BottomSheetDialog`: seed search field from box text; select-all-or-cursor-end and request IME focus (window `SOFT_INPUT_ADJUST_RESIZE` + `STATE_VISIBLE`); TextWatcher re-filters via `TextInputHistoryMatcher` (score desc, then recency); tap → callback replaces box text, cursor at end, dismiss, refocus box and show IME; long-press row → `TextInputHistory.delete`, list refresh; clear-all → `clear()` + placeholder state; "no history yet" vs "no matches" hints; dismiss-without-pick touches nothing.
  4. Add strings (`history sheet hints, clear-all label, content description`) to `values/strings.xml`.
  5. Commit (`feat: add text-input history bottom-sheet picker`).
- **Verification:** compile green; scenario walk of ACs history-picker-affordance + history-picker-search-and-selection + history-entry-deletion-and-clearing.
- **Rollback:** revert commit; picker is new files plus additive wiring in `setupTextInputView`.

## Plan step 4: Unit tests

- **Covers:** T4.1, T4.2, T4.3
- **Pre-conditions:** steps 1–3 compiled.
- **Action:**
  1. `TextInputHistoryTest` — cites `terminal-toolbar.text-input-history-capture` literally: record order, empty-text skip, head-duplicate bump refreshes timestamp + keeps single entry, non-consecutive duplicates distinct (A1), 100-cap eviction of oldest, delete, clear. Reset singleton state between tests.
  2. `TextInputHistoryMatcherTest` — cites `terminal-toolbar.history-picker-search-and-selection`: `gs`→`git status` hit with indices {0,4}, miss cases, case-insensitivity (A3), consecutive-run scoring beats scattered, empty query matches all.
  3. `TextInputHistoryNavigatorTest` — cites `terminal-toolbar.hardware-keyboard-history-cycling`: up walks older, down walks newer, draft saved before first up and restored past newest, user edit resets, empty history inert.
  4. Commit (`test: cover text-input history store, matcher and navigator`).
- **Verification:** `./gradlew :app:testDebugUnitTest` green.
- **Rollback:** tests are new files; revert commit.

## Plan step 5: Full validation, task checkoff, review readiness

- **Covers:** T4.4
- **Pre-conditions:** steps 1–4 committed on the worktree branch.
- **Action:**
  1. Run `./gradlew :app:compileDebugJavaWithJavac :app:testDebugUnitTest` in the worktree; fix regressions (never weaken the gate).
  2. Check off completed tasks in the WORKTREE copy of tasks.md (gate reads tasks worktree-side); verify each task's diff against its file contract (`git diff --name-only` vs `files_allowed`).
  3. Run `opsx gate add-text-input-history --worktree <path>` → remaining failures should be review verdicts only; dispatch blind 2-model code review (doneness rides it — plain M combined dispatch, designated reviewer = first `review` model).
- **Verification:** gate report shows only review-verdict failures before dispatch.
- **Rollback:** n/a (bookkeeping + validation).

## Completion Verification

- `./gradlew :app:compileDebugJavaWithJavac :app:testDebugUnitTest` — exit 0, all tests pass (agent-independent validation source; validation_source_mode: required).
- `opsx gate add-text-input-history --worktree <path>` — exit 0 after review + doneness verdicts seal.
- AC↔test tracing: AC IDs `terminal-toolbar.text-input-history-capture`, `.history-picker-search-and-selection`, `.hardware-keyboard-history-cycling` appear literally in test sources (verify check 5). Picker-affordance and deletion/clearing ACs are UI-bound: covered by code review scenario walk (step 3 verification), not unit-greppable — noted for the verify artifact.

## Manual Adjustments

- Execution Mode `standard`: ordered actions instead of 5-step TDD micro-tasks; tests consolidated in plan step 4.
- UI-heavy ACs (bottom-sheet interaction, icon affordance) have no instrumented-test harness in this repo (no androidTest for app UI); their verification is the blind code review's scenario walk plus the doneness judge — recorded so verify.md check 5 doesn't over-claim.
