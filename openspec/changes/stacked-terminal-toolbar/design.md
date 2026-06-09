## Context

The bottom terminal toolbar is a single `androidx.viewpager.widget.ViewPager` (`R.id.terminal_toolbar_view_pager`) in `activity_termux.xml`. `TerminalToolbarViewPager.PageAdapter` supplies two pages:

- page 0 → `view_terminal_toolbar_extra_keys.xml` = `ExtraKeysView` (writes keys directly to the session/terminal view)
- page 1 → `view_terminal_toolbar_text_input.xml` = `EditText` (soft-keyboard composing; Enter → `session.write(...)`)

`TermuxActivity.setTerminalToolbarView()` sets the adapter and an `OnPageChangeListener` that moves focus between the terminal view (page 0) and the EditText (page 1). `setTerminalToolbarHeight()` sizes the pager to `defaultHeight × extraKeyRowCount × scaleFactor`. `toggleTerminalToolbar()` shows/hides the pager via the `show_extra_keys` SharedPreference.

Constraint: keep the default experience byte-for-byte identical (this is a fork patch meant to merge cleanly with upstream). The new mode is opt-in via a `termux.properties` boolean, matching how all other toolbar config (`extra-keys`, `extra-keys-style`, `terminal-toolbar-height`) is expressed.

## Goals / Non-Goals

**Goals:**
- When `terminal-toolbar-stacked = true`, show the extra-keys row and the text-input box simultaneously, stacked vertically.
- Keep the default (`false`) path identical to current swipeable two-page behavior.
- Correct toolbar height in stacked mode (extra-keys rows + one text-input row, scaled).
- Deterministic focus routing so terminal keys and the text box don't fight over focus.
- Survive config changes (rotation): preserve text-box contents as today.

**Non-Goals:**
- Making extra-key modifier toggles (CTRL/ALT/SHIFT/FN) compose with text typed in the EditText. Out of scope; documented limitation.
- An in-app GUI settings toggle. Property-only by decision.
- Reworking the input/IME subsystem or terminal emulator.

## Decisions

**D1 — Property-driven layout selection, not runtime view surgery on the pager.**
Add `terminal-toolbar-stacked` (default `false`) to `TermuxPropertyConstants` + a getter. At `setTerminalToolbarView()` time, branch: property off → existing `ViewPager` path untouched; property on → use a stacked container. Rationale: smallest blast radius on the default path; the off-branch is a no-op diff at runtime.

**D2 — Stacked container = a sibling view in the layout, toggled by visibility.**
Add a vertical `LinearLayout` (id e.g. `terminal_toolbar_stacked`) in `activity_termux.xml` next to the existing `ViewPager`, both `layout_alignParentBottom`. Exactly one is `VISIBLE`; the other `GONE`, chosen by the property. The stacked layout `<include>`s the same `view_terminal_toolbar_extra_keys` and `view_terminal_toolbar_text_input` layouts so the views, ids, and styling are shared.
- Alternative considered: programmatically build the stack in Java. Rejected — duplicates layout, harder to keep parity with the included views' ids that the rest of the code (`findViewById(R.id.terminal_toolbar_text_input)`) depends on.

**D3 — Reuse `PageAdapter` binding logic via extracted helpers.**
The ExtraKeysView setup (`setExtraKeysViewClient`, `setButtonTextAllCaps`, `setExtraKeysView`, `reload`) and the EditText setup (`OnEditorActionListener` that writes to the session) currently live inside `PageAdapter.instantiateItem`. Extract into static helpers (e.g. `TerminalToolbarViewPager.setupExtraKeysView(activity, view)` / `setupTextInputView(activity, view, savedText)`) so both the pager path and the stacked path call the same code.
- Alternative: copy-paste into `TermuxActivity`. Rejected — drift risk.

**D4 — Height in stacked mode = (extraKeyRows + 1) × defaultHeight × scale.**
`setTerminalToolbarHeight()` gains a stacked branch adding one row for the text box. The included extra-keys view keeps `match_parent`/weight within its share; the EditText takes one row's height. Keep `terminal-toolbar-height` scale applied to the whole stack.

**D5 — Focus routing rules for stacked mode.**
- `isTerminalToolbarTextInputViewSelected()` and `isTerminalViewSelected()` no longer derive from `ViewPager.getCurrentItem()` when stacked (there is no current item). Define: in stacked mode, "text input selected" ⇔ the EditText currently has focus; otherwise terminal is selected.
- `TermuxTerminalViewClient.isTerminalViewSelected()` (line 225) must return true when the terminal view has focus even though the toolbar is present — its existing `mActivity.getTerminalView().hasFocus()` clause already covers this; ensure the pager-null/`isTerminalViewSelected` branch degrades correctly when the pager is `GONE`.
- Tapping an extra key must not steal focus from the terminal view (ExtraKeysView buttons are not focusable in touch mode by default — verify). Tapping the EditText focuses it; on Enter it writes and keeps focus for the next line.

**D6 — Toggle + persistence unchanged in spirit.**
`toggleTerminalToolbar()` shows/hides whichever container is active (pager or stacked) using the same `show_extra_keys` pref. `saveTerminalToolbarTextInput()` already keys off `findViewById(R.id.terminal_toolbar_text_input)`, which resolves in both modes, so rotation persistence needs no change.

## Risks / Trade-offs

- [Modifier keys don't compose with swiped text] → Documented limitation; the path users want (swipe a command, tap ESC/arrows for navigation) still works. No mitigation attempted this change.
- [`isTerminalViewSelected()` regressions causing keys to route to the wrong view] → Cover both modes; rely on `hasFocus()` rather than pager item; manual test matrix (terminal focused, box focused, extra-key tap) before merge.
- [Layout height wrong on multi-row extra keys] → Reuse existing row-count math, add exactly one row; test with single-row and double-row `extra-keys`.
- [Upstream merge conflicts] → Keep the off-branch identical and confine new code to additive branches/helpers; prefix commits `custom:`.
- [`extra-keys-style=none` interaction in stacked mode] → If extra keys are empty, stacked mode should still show the text box (height = 1 row). Verify the row-count-zero case.

## Migration Plan

- Default `false` → zero behavioral change on upgrade; no migration needed.
- Enable: add `terminal-toolbar-stacked = true` to `~/.termux/termux.properties`, run `termux-reload-settings` (or restart activity). Disable: remove the line / set `false` + reload.
- Rollback: property off restores stock behavior; the patch can also be reverted wholesale since the default path is untouched.

## Open Questions

- Stack order: text box below extra keys (recommended, thumb near keyboard) vs above. Defaulting to **below**; revisit if it feels wrong in testing.
- Does `terminal-toolbar-height` scaling feel right applied to the taller stack, or should the text-box row use a fixed height independent of the scale factor? Decide during implementation testing.
