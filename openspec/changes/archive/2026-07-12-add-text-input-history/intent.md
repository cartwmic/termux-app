# Intent: add-text-input-history

## Intent

Add a history feature to the terminal toolbar text-input box (the swipe-typing box above the keyboard). Every non-empty line sent to the terminal via the box is recorded in an in-memory history (per app process, max 100 entries, consecutive duplicates deduplicated by bumping the existing entry to most-recent and refreshing its timestamp). Bare-empty sends (which transmit `\r`) are not recorded.

Users recall history two ways: (1) a history icon rendered at the end of the text-input box opens a Material bottom sheet containing a search field (seeded with the box's current text) and a live fuzzy-filtered list of history entries — subsequence matching (e.g. `gs` matches `git status`), matched characters highlighted, ranked by match score then recency, each entry showing a dim relative timestamp (e.g. "2m ago"); tapping an entry replaces the entire box contents with the entry (cursor at end), dismisses the sheet, and returns focus + IME to the box. (2) With a hardware keyboard, Up/Down while the box is focused cycles history readline-style: the in-progress draft is saved before the first Up and restored when cycling past the newest entry; typing resets the cycle position.

## Constraints

- History is in-memory only — never persisted to disk or SharedPreferences (the box is a plausible place to type passwords). It lives in a process-lifetime holder (not an activity field) so it survives activity recreation but dies with the process.
- The feature works identically in stacked mode (fork default) and legacy paged mode; both wire through the shared `TerminalToolbarViewPager.setupTextInputView`.
- The picker is a bottom sheet (`BottomSheetDialog`, Material 1.12.0 already a dependency) that opens above/over the keyboard; its search field owns the IME while open, and the sheet window uses `adjustResize` so the list is not obscured. Zero fuzzy hits show an empty list with a "no matches" hint (backspace widens).
- The sheet list is most-recent-first with a clear-all footer row; long-pressing an entry deletes it immediately (no confirmation).
- The history icon is the only touch affordance (e.g. `drawableEnd` with a touch-region handler on the existing `EditText` — the view id `terminal_toolbar_text_input` and its consumers must keep working). Long-press on the box retains the stock text-selection/paste menu in all states.
- Dismissing the sheet without picking leaves the box contents untouched.
- Extra-key arrow buttons continue to write escape codes directly to the terminal session; they do NOT cycle history.
- No new external dependencies; no changes to terminal session write semantics.

## Invariants honored

- `openspec/specs/terminal-toolbar/spec.md` — all existing requirements stand unchanged: send-and-clear behavior ("Text box sends line to terminal" scenarios), focus routing (tapping an extra key keeps terminal focus; tapping the box focuses it), show/hide toggle semantics, stacked-mode height sizing, and preservation of unsent text across configuration changes. History capture happens at the existing send point and is purely additive.
- No `openspec/domain.md` or `openspec/constitution.md` exist in this project; the terminal-toolbar spec is the governing baseline.

## Non-goals

- Persisting history to disk (opt-in or otherwise).
- Per-terminal-session history scoping (history is global to the app).
- Changing extra-key routing so toolbar arrow keys cycle history.
- Prefix/substring-only filtering modes (fuzzy subsequence is the single filter behavior).
- Pinning entries, editing entries in place, undo-after-delete, or send-immediately-on-tap.
- Any change to legacy paged-mode vs stacked-mode selection behavior (`terminal-toolbar-stacked` property).
