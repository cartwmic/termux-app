## Why

The terminal toolbar packs the extra-keys row and the swipe-typing text-input box into a single `ViewPager` with two pages, so only one is visible at a time — you swipe left for the text box, swipe right for the extra keys. Users who want soft-keyboard features (swipe typing, autocorrect, prediction) **and** quick access to ESC/CTRL/arrows must constantly swipe between the two. This change lets both appear at once.

## What Changes

- Add an opt-in `termux.properties` boolean `terminal-toolbar-stacked` (default `false`) that, when enabled, renders the extra-keys row and the text-input box stacked vertically in the toolbar instead of as two swipeable pages.
- When disabled (default), behavior is identical to today: a single swipeable `ViewPager` with two pages. **Non-breaking** for existing users.
- Toolbar height in stacked mode accounts for both components: extra-keys rows plus one text-input row, still scaled by `terminal-toolbar-height`.
- Toolbar show/hide toggle (drawer long-press, `Vol-Up`+`K`, fn-`q`/`k`) continues to show or hide the whole toolbar in both modes.
- Focus routing is defined for stacked mode: tapping the terminal or an extra key keeps focus on the terminal view; tapping the text box focuses the text box.
- Known limitation (documented, not fixed here): extra-key modifier toggles (CTRL/ALT/SHIFT/FN) do not compose with text typed into the text-input box, because soft-keyboard input is routed to the EditText rather than the terminal view.

## Capabilities

### New Capabilities
- `terminal-toolbar`: Layout and behavior of the bottom terminal toolbar — its two components (extra-keys row, swipe-typing text-input box), the property-driven choice between swipeable-paged and stacked presentation, height calculation, show/hide toggling, and focus routing.

### Modified Capabilities
<!-- None: no existing OpenSpec spec covers the toolbar (fresh openspec init). -->

## Impact

- **New property**: `terminal-toolbar-stacked` in `TermuxPropertyConstants` (+ getter in `TermuxSharedProperties`/`TermuxAppSharedProperties`).
- **Layout**: `app/src/main/res/layout/activity_termux.xml` — toolbar container must support a stacked variant alongside the existing `ViewPager`.
- **Toolbar wiring**: `TermuxActivity` (`setTerminalToolbarView`, `setTerminalToolbarHeight`, `toggleTerminalToolbar`, `isTerminalViewSelected`, `isTerminalToolbarTextInputViewSelected`) and `TerminalToolbarViewPager` (page-adapter logic reused for direct binding in stacked mode).
- **Key routing**: `TermuxTerminalViewClient.isTerminalViewSelected()` pager-based check.
- **No change** to the terminal emulator, sessions, or input subsystem. Modifier-key composition limitation is inherent and left as-is.
