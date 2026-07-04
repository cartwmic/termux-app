## 1. Manifest — deep-link intent-filter

- [ ] 1.1 Add a VIEW deep-link intent-filter to `TermuxActivity` for scheme
      `termux`, host `zellij-jump` (BROWSABLE + DEFAULT categories). Confirm the
      activity `launchMode` supports single-instance re-delivery (`singleTask`
      or equivalent) so re-delivery routes through the running instance.
  - intent: feature
  - files_allowed:
      - app/src/main/AndroidManifest.xml
  - allow_new_files: false

## 2. Core handler

- [ ] 2.1 Add a jump handler that parses `termux://zellij-jump/<pane-id>`,
      extracts the verbatim pane-id path segment, and — when non-empty —
      dispatches a BACKGROUND command (configured jump script path + pane-id
      arg) via the existing RunCommand/background-exec plumbing; then brings
      `TermuxActivity` to the foreground. Missing/empty pane-id ⇒ foreground
      only. Missing script path ⇒ foreground + log, no crash.
  - intent: feature
  - files_allowed:
      - app/src/main/java/com/termux/app/ZellijJumpHandler.java
      - app/src/main/java/com/termux/app/TermuxActivity.java
  - allow_new_files: true
- [ ] 2.2 Wire intent delivery: handle the jump intent in both the initial
      `onCreate` path and an `onNewIntent` override so an already-running
      activity handles re-delivery without spawning a second instance.
  - intent: feature
  - files_allowed:
      - app/src/main/java/com/termux/app/TermuxActivity.java
  - allow_new_files: false

## 3. Tests

- [ ] 3.1 Add a JVM unit test for the pane-id extraction / URI parsing logic
      (nominal `terminal_7`, empty segment, malformed URI) — pure logic, no
      Android instrumentation required.
  - intent: feature
  - files_allowed:
      - app/src/test/java/com/termux/app/ZellijJumpHandlerTest.java
  - allow_new_files: true
