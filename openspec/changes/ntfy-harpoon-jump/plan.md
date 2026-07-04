# Execution Plan

## Plan step 1: Manifest deep-link intent-filter

- **Covers:** T1.1
- **Pre-conditions:**
  - `app/src/main/AndroidManifest.xml` present; `TermuxActivity` declared.
- **Action:**
  1. Add a VIEW intent-filter under the `TermuxActivity` `<activity>` element:
     `android.intent.action.VIEW` + `DEFAULT` + `BROWSABLE`, `<data
     android:scheme="termux" android:host="zellij-jump" />`.
  2. Verify/confirm the activity `launchMode` routes re-delivery to a single
     instance (`singleTask`); note in Execution Notes if it already does.
- **Verification:**
  - `openspec validate --changes --strict` (manifest is not validated by opsx,
    but keeps the change green); `./gradlew :app:assembleDebug` (warn-only)
    when toolchain available.
- **Rollback:** revert the intent-filter block.

## Plan step 2: Jump handler + intent wiring

- **Covers:** T2.1, T2.2
- **Pre-conditions:** Step 1 landed.
- **Action:**
  1. Create `ZellijJumpHandler` with a pure `extractPaneId(Uri)` returning the
     verbatim host-scoped path segment or null/empty.
  2. Implement `handle(Context, Intent)`: on non-empty pane-id, build a
     background RUN_COMMAND (background=true) to the configured jump-script path
     with the pane-id arg; always foreground `TermuxActivity`.
  3. Call the handler from `TermuxActivity.onCreate` (initial intent) and add an
     `onNewIntent` override delegating to the same handler.
  4. Graceful paths: empty pane-id ⇒ foreground only; missing script ⇒
     foreground + log.
- **Verification:**
  - Unit test (step 3) green; `assembleDebug` warn-only.
- **Rollback:** remove `ZellijJumpHandler`, revert `TermuxActivity` edits.

## Plan step 3: Unit test for parsing

- **Covers:** T3.1
- **Pre-conditions:** Step 2 landed.
- **Action:**
  1. Add `ZellijJumpHandlerTest` asserting `extractPaneId` on `terminal_7`
     (returns `terminal_7`), empty segment (returns null/empty), and malformed
     URI (returns null/empty, no throw).
- **Verification:**
  - `./gradlew :app:testDebugUnitTest --tests '*ZellijJumpHandlerTest'`
    (warn-only in gates; run when toolchain available).
- **Rollback:** remove the test file.

## Completion Verification

- `openspec validate --changes --strict` exits 0.
- All tasks.md items checked.
- `./gradlew :app:assembleDebug` succeeds when the Android SDK + keystore are
  available (warn-only gate; not a hard blocker in CI-less environments).

## Manual Adjustments

- ssh target, remote host, and the `zellij pipe` invocation are NOT baked into
  the fork — they live in a user-space jump script the handler invokes by
  configured path (Constitution III: no hosts/secrets in source). The fork
  passes only the pane-id.
