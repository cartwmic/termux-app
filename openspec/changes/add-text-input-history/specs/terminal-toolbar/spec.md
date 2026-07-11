# Capability: terminal-toolbar

## ADDED Requirements

### Requirement: Text-Input History Capture

WHEN text is sent to the terminal from the text-input box, THE system SHALL record the sent text with a capture timestamp in an in-memory, process-lifetime history, subject to: empty sends (which transmit `\r`) are not recorded; a send whose text equals the current most-recent entry SHALL bump that entry's timestamp instead of inserting a duplicate; the history SHALL hold at most 100 entries, evicting the oldest on overflow. THE system SHALL NOT persist history to disk, SharedPreferences, or logs.

#### Scenario: Sent line is recorded

- **WHEN** the user sends the text `git status` from the text-input box
- **THEN** `git status` becomes the most-recent history entry with a fresh timestamp, and the terminal write and box-clear behavior are unchanged

#### Scenario: Empty send is not recorded

- **WHEN** the user triggers the send action with an empty text-input box (transmitting `\r`)
- **THEN** no history entry is added

#### Scenario: Consecutive duplicate bumps instead of inserting

- **WHEN** the user sends `make test` and the most-recent history entry is already `make test`
- **THEN** the history still contains one `make test` entry at the most-recent position with a refreshed timestamp

#### Scenario: Capacity eviction

- **WHEN** a new line is sent while the history already holds 100 entries
- **THEN** the oldest entry is evicted and the new entry becomes most-recent

#### Scenario: History survives activity recreation but not process death

- **WHEN** the device is rotated (activity recreation) after entries were captured
- **THEN** the history is intact; after the app process is killed and restarted, the history is empty

#### Scenario: Capture works in both toolbar modes

- **WHEN** text is sent from the text-input box in stacked mode or in legacy paged mode
- **THEN** the same history records the entry identically in either mode

### Requirement: History Picker Affordance

THE system SHALL display a history icon at the end of the text-input box in both toolbar modes; tapping it SHALL open the history picker. THE system SHALL NOT alter the box's long-press behavior (the stock text-selection/paste menu remains in all states), and the icon SHALL be present whether or not the history is empty.

#### Scenario: Icon opens the picker

- **WHEN** the user taps the history icon in the text-input box
- **THEN** the history picker opens

#### Scenario: Long-press remains the stock menu

- **WHEN** the user long-presses the text-input box, with or without text present
- **THEN** the standard text-selection/paste behavior occurs and the history picker does not open

#### Scenario: Empty history shows placeholder

- **IF** the history is empty when the icon is tapped
- **THEN** the picker opens showing a "no history yet" placeholder instead of an entry list

### Requirement: History Picker Search And Selection

THE history picker SHALL be a bottom sheet containing a search field and the history entry list ordered most-recent first, each entry showing its text and a relative timestamp. The search field SHALL be seeded with the text-input box's current contents, SHALL own keyboard focus while the sheet is open, and SHALL filter the list live using case-insensitive fuzzy subsequence matching with matched characters highlighted, ranked by match score then recency. WHEN the user taps an entry, THE system SHALL replace the entire text-input box contents with that entry, place the cursor at the end, dismiss the sheet, and return focus and the soft keyboard to the text-input box. THE sheet layout SHALL keep the filtered list visible while the soft keyboard is open.

#### Scenario: Seeded live fuzzy filtering

- **WHEN** the box contains `gs` and the user taps the history icon
- **THEN** the sheet opens with `gs` in the search field and the list showing only entries that fuzzy-match `gs` (e.g. `git status`), matched characters highlighted, ranked by match score then recency

#### Scenario: Refining the query narrows the list

- **WHEN** the sheet is open and the user types additional characters into the search field
- **THEN** the list re-filters live without closing the sheet or the keyboard

#### Scenario: Picking an entry fills the box

- **WHEN** the user taps the entry `git status` in the sheet
- **THEN** the text-input box contents become exactly `git status` with the cursor at the end, the sheet dismisses, and focus plus the soft keyboard return to the box

#### Scenario: No fuzzy matches

- **IF** the search query fuzzy-matches no history entry
- **THEN** the sheet shows an empty list with a "no matches" hint, and deleting query characters widens the results again

#### Scenario: Dismissing without picking

- **WHEN** the user dismisses the sheet without selecting an entry
- **THEN** the text-input box contents are exactly as they were before the sheet opened

### Requirement: History Entry Deletion And Clearing

THE history picker SHALL support removing a single entry via long-press on that entry (immediate, no confirmation) and SHALL provide a clear-all footer row that removes all entries.

#### Scenario: Long-press deletes one entry

- **WHEN** the user long-presses a history entry in the sheet
- **THEN** that entry is removed from the history immediately and the list updates

#### Scenario: Clear-all empties the history

- **WHEN** the user taps the clear-all footer row
- **THEN** all history entries are removed and the picker shows the empty-history placeholder

### Requirement: Hardware Keyboard History Cycling

WHILE the text-input box has focus, WHEN a hardware keyboard Up or Down key is pressed, THE system SHALL cycle the box contents through history readline-style: Up moves to older entries, Down to newer; the in-progress draft SHALL be saved before the first Up and restored when cycling past the newest entry; typing SHALL reset the cycle position. THE system SHALL NOT change extra-key arrow buttons, which continue to write escape codes to the terminal session.

#### Scenario: Up recalls the previous entry

- **WHEN** the box is focused and the user presses hardware Up
- **THEN** the box contents are replaced with the most-recent history entry (or the next-older entry on subsequent presses)

#### Scenario: Down past newest restores the draft

- **WHEN** the user had typed an unsent draft, pressed Up one or more times, and then presses Down past the newest history entry
- **THEN** the original draft is restored in the box

#### Scenario: Extra-key arrows unaffected

- **WHEN** the user taps an arrow extra-key while the terminal view has focus
- **THEN** the escape sequence is written to the terminal session and no history cycling occurs

#### Scenario: Cycling with empty history is inert

- **IF** the history is empty when Up is pressed in the focused box
- **THEN** the box contents are unchanged

---

## Acceptance criterion quality checklist

| AC ID | Testable | Solution-free | Unambiguous | Consistent | Complete |
|---|---|---|---|---|---|
| terminal-toolbar.text-input-history-capture | [x] | [x] | [x] | [x] | [x] |
| terminal-toolbar.history-picker-affordance | [x] | [x] | [x] | [x] | [x] |
| terminal-toolbar.history-picker-search-and-selection | [x] | [x] (bottom-sheet surface is a frozen intent decision, not implementation leakage) | [x] | [x] | [x] |
| terminal-toolbar.history-entry-deletion-and-clearing | [x] | [x] | [x] | [x] | [x] |
| terminal-toolbar.hardware-keyboard-history-cycling | [x] | [x] | [x] | [x] | [x] |
