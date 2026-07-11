<!-- authored: in-session -->

## Why

The terminal toolbar text-input box discards every line after sending it — retyping recent commands on a soft keyboard is the single most repetitive interaction in the app. An app-level history (distinct from shell history, which the app never touches — see domain "Out-of-scope") removes that friction while honoring constitution V: input is retained in memory only, never persisted.

## What Changes

- Record every non-empty line sent from the text-input box into an in-memory, process-lifetime history (cap 100, consecutive-duplicate sends bump the existing entry and refresh its timestamp; bare-empty sends that transmit `\r` are not recorded).
- Add a history icon at the end of the text-input box (the sole touch affordance; box long-press keeps the stock selection/paste menu).
- Icon opens a Material `BottomSheetDialog`: search field seeded with the box's current text, live fuzzy-filtered (subsequence match, matched characters highlighted, ranked match score then recency) entry list with dim relative timestamps, and a clear-all footer row. Tap entry → replaces the entire box contents (cursor at end), sheet dismisses, focus + IME return to the box. Long-press entry → immediate delete. Zero hits → empty list with "no matches" hint. Dismissing without picking leaves the box untouched.
- Hardware-keyboard Up/Down while the box is focused cycles history readline-style (draft saved before first Up, restored past newest; typing resets the cycle).
- Behavior is identical in stacked and paged toolbar modes — wired once through the shared `TerminalToolbarViewPager.setupTextInputView` (constitution IV).

## Capabilities

### New Capabilities

- (none)

### Modified Capabilities

- `terminal-toolbar`: ADDED requirements for text-input history capture, the bottom-sheet history picker (icon affordance, fuzzy search, pick/delete/clear semantics), and hardware-keyboard history cycling. All existing requirements (send-and-clear, focus routing, show/hide, height sizing, rotation preservation) are unchanged.

## Impact

Affected files (expected):

- `app/src/main/java/com/termux/app/terminal/io/TerminalToolbarViewPager.java` — capture hook in `setupTextInputView` send path; icon + key-listener wiring (shared by both modes).
- `app/src/main/java/com/termux/app/terminal/io/` — new history store class (in-memory singleton: entries `(text, epochMillis)`, cap/dedupe/delete/clear) and bottom-sheet picker class (fuzzy matching + list adapter).
- `app/src/main/res/layout/view_terminal_toolbar_text_input.xml` — `drawableEnd` history icon on the existing `EditText` (id `terminal_toolbar_text_input` unchanged; consumers unaffected).
- New layout for the bottom sheet (search field + list + clear-all footer) and new drawable/string resources.
- No manifest, dependency, or terminal-emulator/termux-shared changes: Material 1.12.0 is already a dependency; session write semantics untouched (constitution II).

Affects this repo only.

## Open Questions

Folded clarify (plain Scale M). All questions raised during explore were resolved with the user before intent freeze; recorded here with their rulings:

1. **Recall UX** — resolved: icon-only touch affordance + hardware Up/Down cycling. Long-press on the box is NOT overloaded (stock paste menu preserved in all states).
2. **Persistence** — resolved: in-memory only, no disk/opt-in variant (non-goal; constitution V).
3. **Pick semantics** — resolved: fill box (replace entire contents, cursor at end), never send-immediately.
4. **Picker surface** — resolved: bottom sheet with its own search field and live fuzzy narrowing; one-shot filtering rejected.
5. **Filter algorithm** — resolved: fuzzy subsequence (not prefix/substring), score-then-recency ranking.
6. **List extras** — resolved: relative timestamps shown; long-press-row delete (no confirmation, no undo); clear-all footer row.

Open assumptions (author-resolved, no blocker):

- A1: "Consecutive dedupe" is implemented as: if the sent text equals the current most-recent entry, bump/refresh rather than insert — duplicates separated by other entries remain distinct. (Matches intent wording "consecutive duplicates deduplicated by bumping".)
- A2: The history icon renders whether history is empty or not; empty history opens the sheet with a "No history yet" placeholder (stable affordance, per explore).
- A3: Fuzzy match is case-insensitive (soft-keyboard capitalization is unreliable); ranking prefers denser/earlier matches, ties broken by recency. Exact scoring weights are implementation detail bounded by the spec scenarios.

Design.md: not authored — decision-gated at plain M (D5); no decision here passes the ADR 4-point test (all consequential choices are frozen in intent.md and expressed as spec scenarios).
