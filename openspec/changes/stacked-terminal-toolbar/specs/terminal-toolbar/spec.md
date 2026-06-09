## ADDED Requirements

### Requirement: Stacked toolbar property

The system SHALL provide a `terminal-toolbar-stacked` boolean property in `termux.properties` that selects how the terminal toolbar presents its extra-keys row and text-input box. The property SHALL default to `false`. When `false`, the toolbar SHALL behave exactly as the legacy two-page swipeable `ViewPager`. When `true`, the toolbar SHALL present both components stacked vertically and simultaneously visible.

#### Scenario: Property absent uses legacy paged toolbar
- **WHEN** `terminal-toolbar-stacked` is not set in `termux.properties`
- **THEN** the toolbar presents a single swipeable view showing either the extra-keys row or the text-input box, one at a time, identical to prior behavior

#### Scenario: Property enabled selects stacked toolbar
- **WHEN** `terminal-toolbar-stacked = true` is set and settings are reloaded
- **THEN** the toolbar shows the extra-keys row and the text-input box at the same time, stacked vertically

#### Scenario: Property toggled off restores legacy toolbar
- **WHEN** `terminal-toolbar-stacked` is changed from `true` to `false` and settings are reloaded
- **THEN** the toolbar reverts to the swipeable two-page presentation with no residual stacked layout

### Requirement: Simultaneous extra-keys and text-input in stacked mode

When stacked mode is active, the system SHALL render both the extra-keys row and the swipe-typing text-input box without requiring a swipe gesture to switch between them. Both components SHALL use the same view ids, styling, and key/text handling as the legacy paged components.

#### Scenario: Both components visible without swiping
- **WHEN** stacked mode is active and the toolbar is shown
- **THEN** the extra-keys row and the text-input box are both visible and interactable with no swipe required

#### Scenario: Extra key sends to terminal in stacked mode
- **WHEN** stacked mode is active and the user taps an extra key (e.g. ESC, TAB, an arrow)
- **THEN** the key is written to the current terminal session, same as in legacy mode

#### Scenario: Text box sends line to terminal in stacked mode
- **WHEN** stacked mode is active, the user types text into the text-input box and presses the send/enter action
- **THEN** the text is written to the current terminal session and the text box is cleared

### Requirement: Toolbar height accounts for both components in stacked mode

In stacked mode the system SHALL size the toolbar to fit the extra-keys rows plus one text-input row, scaled by the `terminal-toolbar-height` factor. When the extra-keys set is empty (e.g. `extra-keys-style=none`), the toolbar SHALL still allocate height for the text-input box.

#### Scenario: Height fits multi-row extra keys plus text box
- **WHEN** stacked mode is active and `extra-keys` defines two rows
- **THEN** the toolbar height fits two extra-key rows plus one text-input row, scaled by `terminal-toolbar-height`

#### Scenario: Text box still shown when extra keys are empty
- **WHEN** stacked mode is active and the extra-keys set resolves to zero rows
- **THEN** the toolbar still displays the text-input box with a height of one row

### Requirement: Show/hide toggle applies to stacked toolbar

The existing toolbar show/hide controls (drawer keyboard long-press, `Vol-Up`+`K`, fn-`q`/`k`) SHALL show or hide the entire toolbar in stacked mode, governed by the same `show_extra_keys` preference used in legacy mode.

#### Scenario: Toggle hides the whole stacked toolbar
- **WHEN** stacked mode is active and the user triggers the toolbar hide toggle
- **THEN** both the extra-keys row and the text-input box are hidden together

#### Scenario: Toggle reveals the whole stacked toolbar
- **WHEN** stacked mode is active, the toolbar is hidden, and the user triggers the toolbar show toggle
- **THEN** both the extra-keys row and the text-input box become visible together

### Requirement: Focus routing in stacked mode

In stacked mode the system SHALL route key input deterministically: when the text-input box has focus, input is treated as text-input selection; otherwise the terminal view is treated as selected and receives terminal key handling. Tapping an extra key SHALL NOT move focus away from the terminal view.

#### Scenario: Tapping the text box focuses it
- **WHEN** stacked mode is active and the user taps the text-input box
- **THEN** the text-input box gains focus and soft-keyboard input is composed into it

#### Scenario: Tapping an extra key keeps terminal focus
- **WHEN** stacked mode is active, the terminal view has focus, and the user taps an extra key
- **THEN** the key is sent to the terminal and focus remains on the terminal view

### Requirement: Text-input contents preserved across configuration changes

In stacked mode the system SHALL preserve unsent text-input box contents across activity recreation (e.g. rotation), consistent with legacy behavior.

#### Scenario: Rotation preserves unsent text
- **WHEN** stacked mode is active, the user has typed unsent text in the text-input box, and the device is rotated
- **THEN** the previously typed text is restored in the text-input box after recreation
