# Capability: notification-jump

## ADDED Requirements

### Requirement: Notification Jump Deep Link

WHEN a `termux://zellij-jump/<pane-id>` VIEW intent is delivered, THE app SHALL foreground the terminal activity and dispatch the configured jump command in the background with the `<pane-id>` value passed verbatim.

#### Scenario: Tapping a jump notification
- **WHEN** a `termux://zellij-jump/terminal_7` VIEW intent is delivered
- **THEN** `TermuxActivity` is brought to the foreground
- **AND** the configured jump command is dispatched in the background with
  argument `terminal_7`

#### Scenario: Missing pane id
- **IF** the delivered `termux://zellij-jump/` intent carries no non-empty
  pane-id path segment
- **THEN** THE app SHALL foreground the activity WITHOUT dispatching any
  background command, and SHALL NOT crash

### Requirement: Background Single-Session Reuse

WHEN dispatching the jump command, THE app SHALL run it as a BACKGROUND command
(no new visible terminal session), so the single persistent ssh/zellij session
remains the foregrounded session.

#### Scenario: Background dispatch preserves the live session
- **WHEN** the jump command is dispatched
- **THEN** it runs with background execution (no new visible Termux session is
  created)
- **AND** foregrounding the app lands on the pre-existing live session

#### Scenario: Configured jump command absent
- **IF** the configured jump command / script path does not exist on the device
- **THEN** THE app SHALL still foreground the activity and SHALL log the missing
  path WITHOUT crashing

### Requirement: Running-Activity Re-delivery

WHILE `TermuxActivity` is already running, THE app SHALL handle an arriving `termux://zellij-jump/<pane-id>` intent via the running instance (single-task re-delivery) rather than creating a second activity instance, preserving the single live session.

#### Scenario: Jump while Termux already open
- **WHILE** `TermuxActivity` is already the running foreground/background task
- **WHEN** a jump deep link arrives
- **THEN** the existing activity instance handles the new intent
- **AND** no second `TermuxActivity` instance is created

---

## Acceptance criterion quality checklist

| AC ID | Testable | Solution-free | Unambiguous | Consistent | Complete |
|---|---|---|---|---|---|
| notification-jump.notification-jump-deep-link | [x] | [x] | [x] | [x] | [x] |
| notification-jump.background-single-session-reuse | [x] | [x] | [x] | [x] | [x] |
| notification-jump.running-activity-re-delivery | [x] | [x] | [x] | [x] | [x] |
