## 1. Property plumbing

- [x] 1.1 Add `KEY_TERMINAL_TOOLBAR_STACKED = "terminal-toolbar-stacked"` and `DEFAULT_IVALUE_TERMINAL_TOOLBAR_STACKED = false` to `TermuxPropertyConstants` (near the existing toolbar/extra-keys keys)
- [x] 1.2 Register the key in the relevant property sets in `TermuxPropertyConstants` (boolean properties list) so it is parsed as a boolean
- [x] 1.3 Add `isTerminalToolbarStacked()` getter to `TermuxSharedProperties`/`TermuxAppSharedProperties` reading the new key
- [x] 1.4 Document the property in `docs`/`termux.properties` sample comments alongside `terminal-toolbar-height`

## 2. Layout

- [x] 2.1 In `activity_termux.xml`, add a vertical `LinearLayout` `@+id/terminal_toolbar_stacked` as a sibling of the existing `ViewPager`, both `layout_alignParentBottom`, default `visibility=gone`
- [x] 2.2 In the stacked layout, `<include>` `view_terminal_toolbar_extra_keys` and `view_terminal_toolbar_text_input` in the chosen order (text box below extra keys)
- [x] 2.3 Verify the included views keep ids `terminal_toolbar_extra_keys` and `terminal_toolbar_text_input` so existing `findViewById` calls resolve in both modes

## 3. Reusable binding helpers

- [x] 3.1 Extract ExtraKeysView setup from `PageAdapter.instantiateItem` into a static `setupExtraKeysView(TermuxActivity, View)` in `TerminalToolbarViewPager`
- [x] 3.2 Extract text-input EditText setup (incl. `OnEditorActionListener` writing to the session) into a static `setupTextInputView(TermuxActivity, View, String savedText)`
- [x] 3.3 Update `PageAdapter.instantiateItem` to call the two helpers (no behavior change in paged mode)

## 4. Activity wiring

- [x] 4.1 In `TermuxActivity.setTerminalToolbarView()`, branch on `isTerminalToolbarStacked()`: off → existing pager path; on → bind extra-keys + text-input via the helpers into the stacked container, set its visibility from `shouldShowTerminalToolbar()`, hide the pager
- [x] 4.2 Update `setTerminalToolbarHeight()` to compute stacked height as `(extraKeyRows + 1) × defaultHeight × scale`, handling zero extra-key rows (text box still 1 row)
- [x] 4.3 Update `toggleTerminalToolbar()` to show/hide the active container (pager or stacked) using the same `show_extra_keys` pref
- [x] 4.4 Update `isTerminalToolbarTextInputViewSelected()` and `isTerminalViewSelected()` to derive from EditText focus when stacked (no `getCurrentItem()`), guarding against a `GONE`/absent pager

## 5. Key routing

- [x] 5.1 Audit `TermuxTerminalViewClient.isTerminalViewSelected()` (pager-null / selected check) so it returns correct results when the pager is `GONE` and the stacked container is active
- [x] 5.2 Confirm ExtraKeysView buttons do not steal focus from the terminal view in stacked mode (not focusable in touch mode); adjust if needed

## 6. Verification

- [x] 6.1 Build the app (`./gradlew assembleDebug`) and resolve any compile errors
- [ ] 6.2 Manual test matrix — default (property unset): swipe between extra keys and text box works exactly as before
- [ ] 6.3 Manual test matrix — stacked on: both visible; extra keys send to terminal; text box + enter sends a line; toggle hides/shows both; rotation preserves unsent text
- [ ] 6.4 Manual test — stacked on with single-row, double-row, and `extra-keys-style=none`: height correct, text box always present
- [ ] 6.5 Confirm documented limitation holds: CTRL/ALT + swiped text does not compose (expected), standalone special keys do work
