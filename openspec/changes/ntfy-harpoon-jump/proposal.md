<!-- authored: in-session -->

## Why

A process running in a remote zellij pane sends an ntfy notification to the
phone; today tapping it opens Termux but leaves the user to hand-navigate the
live zellij session to the alerting pane. This slice makes a tapped notification
deterministically jump the already-attached zellij session to the exact pane —
reusing the single persistent Termux ssh session, with no fresh login and no new
visible Termux session (see intent.md).

## What Changes

- Add a **notification-jump deep link** handled by the termux-app fork: a tapped
  ntfy notification (click action `termux://zellij-jump/<pane-id>` and/or an
  equivalent broadcast) carries the stable zellij pane id.
- On receipt, the fork runs a **background side-channel** command
  (`RUN_COMMAND_BACKGROUND=true`, no new visible session):
  `ssh <remote> 'zellij pipe --name jump_pane --plugin file:.../harpoon.wasm <pane-id>'`,
  reusing the existing interactive ssh as an SSH ControlMaster connection.
- **Foreground** `TermuxActivity` so the user lands on the single live session,
  already focused on the target pane by the side-channel.
- Degrade gracefully when the harpoon `jump_pane` pipe (harpoon slice) or the
  ControlMaster config (chezmoi slice) is absent: foreground only, no crash.

## Capabilities

### New Capabilities
- `notification-jump`: receive a notification-jump deep link / broadcast carrying
  a zellij pane id, run the background side-channel jump exec against the existing
  ssh ControlMaster connection, and foreground the live Termux session.

### Modified Capabilities
- (none — no existing termux-app OpenSpec capability changes behavior)

## Impact

- **Affects which projects:** termux-app fork (this repo). Consumes primitives
  from sibling slices: harpoon (`jump_pane`/`slot_for_pane` pipes) and chezmoi
  (phone ControlMaster config + remote notify script). Those are out of scope here.
- **Affected files (anticipated):**
  - `app/src/main/AndroidManifest.xml` — deep-link intent-filter (scheme `termux`,
    host `zellij-jump`) and/or broadcast receiver declaration.
  - A new handler (activity/receiver) that parses the pane id, dispatches the
    background `RUN_COMMAND` side-channel exec, and brings `TermuxActivity` forward.
  - Existing `TermuxActivity` / RunCommand plumbing under
    `app/src/main/java/com/termux/app/` — foregrounding + background command entry.
- **Signing invariant:** any APK cut from this change MUST be signed with
  `~/keys/termux-fork.jks` (sharedUserId=com.termux); breaking this breaks
  termux-app ↔ termux-api IPC (intent.md invariant).
- **Gate:** `openspec validate --changes --strict` (required); `./gradlew
  :app:assembleDebug` is warn-only (heavy; needs Android SDK/keystore).

## Open Questions

<!-- clarify folds here at plain M. Assumptions recorded; resolved in-loop unless
they change intent MEANING (then halt). -->

- **Trigger surface — deep-link vs broadcast.** Assumption: implement the
  `termux://zellij-jump/<pane-id>` VIEW deep link on `TermuxActivity` as the
  primary trigger (single tap foregrounds + runs the exec), since it foregrounds
  the activity in one action. A broadcast→RunCommandService forwarder is a
  possible secondary path but not required for the intent's outcome. Resolved:
  deep link primary.
- **Pane-id encoding.** Zellij pane ids are `terminal_N`; the `_` is URL-safe, so
  `termux://zellij-jump/terminal_7` needs no escaping. Assumption: pass the raw
  `$ZELLIJ_PANE_ID` value verbatim in the path segment.
- **Remote host + harpoon plugin path.** These are deployment specifics owned by
  the chezmoi slice (notify script) and the user's ssh config; this slice treats
  the ssh target and `zellij pipe` invocation as configuration, not hardcoded
  values (Constitution III — no secrets/hosts baked into the fork).
