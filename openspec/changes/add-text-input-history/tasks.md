<!-- authored: in-session -->

## 1. Core history model (pure Java, unit-testable)

- [ ] 1.1 Create `TextInputHistory` in-memory store: process-lifetime singleton holding `(text, epochMillis)` entries most-recent-first; `record(text)` ignores empty text, bumps+refreshes when equal to the current most-recent entry, evicts oldest beyond 100; `delete(entry)`, `clear()`, `snapshot()` accessors. No disk/SharedPreferences/log writes (constitution V; AC terminal-toolbar.text-input-history-capture).
  - intent: feature
  - files_allowed:
      - app/src/main/java/com/termux/app/terminal/io/TextInputHistory.java
  - allow_new_files: true
- [ ] 1.2 Create case-insensitive fuzzy subsequence matcher used by the picker: returns match/no-match, matched character indices (for highlighting), and a score ranking denser/earlier matches; ties broken by recency at the call site (AC terminal-toolbar.history-picker-search-and-selection).
  - intent: feature
  - files_allowed:
      - app/src/main/java/com/termux/app/terminal/io/TextInputHistoryMatcher.java
  - allow_new_files: true

## 2. Capture and hardware-keyboard cycling (shared wiring)

- [ ] 2.1 Hook history capture into the shared send path in `TerminalToolbarViewPager.setupTextInputView`: record non-empty sent text before the box is cleared; empty sends (`\r`) are not recorded; send/clear/finished-session semantics unchanged (ACs terminal-toolbar.text-input-history-capture; domain invariants 1–2, 4).
  - intent: feature
  - files_allowed:
      - app/src/main/java/com/termux/app/terminal/io/TerminalToolbarViewPager.java
  - allow_new_files: false
- [ ] 2.2 Add hardware Up/Down readline-style cycling on the focused text-input box: draft saved before first Up, restored when cycling past newest, typing resets the cycle, inert when history is empty; no change to extra-key handling (AC terminal-toolbar.hardware-keyboard-history-cycling).
  - intent: feature
  - files_allowed:
      - app/src/main/java/com/termux/app/terminal/io/TerminalToolbarViewPager.java
      - app/src/main/java/com/termux/app/terminal/io/TextInputHistoryNavigator.java
  - allow_new_files: true

## 3. History picker UI (icon + bottom sheet)

- [ ] 3.1 Add the history icon as `drawableEnd` on the text-input `EditText` with a touch-region handler that opens the picker; view id `terminal_toolbar_text_input` and long-press/selection behavior unchanged; icon present in both toolbar modes and when history is empty (AC terminal-toolbar.history-picker-affordance).
  - intent: feature
  - files_allowed:
      - app/src/main/java/com/termux/app/terminal/io/TerminalToolbarViewPager.java
      - app/src/main/res/layout/view_terminal_toolbar_text_input.xml
      - app/src/main/res/drawable/ic_history.xml
  - allow_new_files: true
- [ ] 3.2 Build the bottom-sheet picker (`BottomSheetDialog`, Material already a dependency): layout with search field + entry list + clear-all footer; search seeded from box text, owns IME focus, live fuzzy filter with highlighted matches and dim relative timestamps, most-recent-first score-then-recency order; tap → replace box contents (cursor at end), dismiss, refocus box + IME; long-press row → immediate delete; clear-all → empty; "no history yet" and "no matches" states; sheet resizes so the list stays visible above the keyboard; dismiss-without-pick leaves the box untouched (ACs terminal-toolbar.history-picker-search-and-selection, terminal-toolbar.history-entry-deletion-and-clearing, terminal-toolbar.history-picker-affordance).
  - intent: feature
  - files_allowed:
      - app/src/main/java/com/termux/app/terminal/io/TextInputHistorySheet.java
      - app/src/main/res/layout/view_text_input_history_sheet.xml
      - app/src/main/res/layout/item_text_input_history_entry.xml
      - app/src/main/res/values/strings.xml
      - app/src/main/java/com/termux/app/terminal/io/TerminalToolbarViewPager.java
  - allow_new_files: true

## 4. Tests and validation

- [ ] 4.1 Unit tests for `TextInputHistory` citing AC ID `terminal-toolbar.text-input-history-capture` literally: record, empty-send exclusion, consecutive-duplicate bump (non-consecutive duplicates stay distinct — proposal A1), 100-cap eviction, delete, clear.
  - intent: feature
  - files_allowed:
      - app/src/test/java/com/termux/app/terminal/io/TextInputHistoryTest.java
  - allow_new_files: true
- [ ] 4.2 Unit tests for the fuzzy matcher citing AC ID `terminal-toolbar.history-picker-search-and-selection`: subsequence hit/miss, case-insensitivity (proposal A3), highlight indices, score ordering.
  - intent: feature
  - files_allowed:
      - app/src/test/java/com/termux/app/terminal/io/TextInputHistoryMatcherTest.java
  - allow_new_files: true
- [ ] 4.3 Unit tests for the cycling navigator citing AC ID `terminal-toolbar.hardware-keyboard-history-cycling`: up/down traversal, draft save/restore, typing resets position, empty-history inert.
  - intent: feature
  - files_allowed:
      - app/src/test/java/com/termux/app/terminal/io/TextInputHistoryNavigatorTest.java
  - allow_new_files: true
- [ ] 4.4 Run the validation source: `./gradlew :app:compileDebugJavaWithJavac :app:testDebugUnitTest` green in the worktree.
  - intent: infra
  - files_allowed:
      - app/**
  - allow_new_files: false
