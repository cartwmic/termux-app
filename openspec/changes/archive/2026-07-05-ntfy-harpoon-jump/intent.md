# Intent — ntfy-harpoon-jump (termux-app slice)

Part of the cross-repo `ntfy-harpoon-jump` feature: tapping an ntfy notification
on the phone jumps the live remote zellij session to the exact tab/pane of the
alerting process. This slice is the phone-side fork work. Sibling slices live in
`harpoon` (pipe primitives) and `chezmoi` (SSH ControlMaster + remote notify script).

## Intent

Extend the termux-app fork so a tapped ntfy notification does two things against
the SINGLE, already-running Termux session that holds the interactive
`ssh → remote zellij`:

1. Background side-channel: run `ssh remote 'zellij pipe --name jump_pane
   --plugin file:.../harpoon.wasm <paneid>'` invisibly (RUN_COMMAND-background,
   no new visible session), reusing the existing SSH ControlMaster connection so
   there is no fresh login and no new zellij client.
2. Foreground: bring `TermuxActivity` to the front, landing on the one live
   session — which the side-channel has already focused onto the right pane.

Trigger surface is a fork-added deep-link/intent (e.g. `termux://zellij-jump/<paneid>`
as the ntfy click action, and/or a broadcast→RunCommandService forwarder), carrying
the stable pane id emitted by the alerting process.

## Constraints

- MUST reuse the existing single interactive session; MUST NOT spawn a new
  visible Termux session or open a fresh interactive ssh to zellij.
- The switch exec MUST run background-only so foregrounding lands on the live
  ssh/zellij session, not a throwaway one.
- Notification carries a stable **pane id**, not a slot number (jump is
  reassignment-immune; see harpoon `jump_pane`).
- Depends on phone-side SSH ControlMaster being configured (chezmoi slice) and
  harpoon `jump_pane` existing (harpoon slice) — degrade gracefully if absent.

## Invariants honored

- **sharedUserId=com.termux hard invariant**: this fork and the termux-api fork
  MUST both be signed with the identical keystore (`~/keys/termux-fork.jks`,
  SHA256 `EC:1A:B3:F5:...:C0:C1`). Any APK cut from this change uses
  `TERMUX_KEYSTORE=~/keys/termux-fork.jks` + password via
  `op read 'op://developer/Termux Fork Signing Key/password'`. Breaking this
  breaks termux-app ↔ termux-api IPC.
- In-place upgrade over F-Droid-signed Termux is impossible (cert mismatch); fork
  install requires full uninstall of `com.termux`/`com.termux.api` — no regression
  to that constraint.

## Non-goals

- No harpoon plugin changes (harpoon slice).
- No SSH client config or remote notify script (chezmoi slice).
- No PTY key-injection path (rejected in favor of the deterministic side-channel).
- No multi-session switching UX (single-session assumption is intentional).
