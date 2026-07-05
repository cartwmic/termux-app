# Follow-ups — ntfy-harpoon-jump

Advisory findings (P2/P3) surfaced during gating review. Not required by the
frozen intent's outcomes; recorded as warnings, never gating. Explore input for a
successor change.

## Advisory

- **P2 (out-of-scope / user-script contract)** — The exported `VIEW`
  intent-filter lets any app fire `termux://zellij-jump/<x>`, passing an
  unsanitized pane id verbatim to the user-space `~/bin/zellij-jump` script. Safe
  at the fork boundary (`EXTRA_ARGUMENTS` is `String[]` argv, no shell → no
  command injection). Residual: the out-of-scope jump script (chezmoi slice) must
  treat `$1` as untrusted and quote it. Route to the chezmoi/remote-script slice.
  — reviewer A, round 1.
- **P3 (idempotent re-dispatch)** — `TermuxActivity.onCreate` calls
  `handle(getIntent())` without a `savedInstanceState == null` guard and does not
  consume the VIEW intent, so a process-death/system recreation can re-dispatch
  the jump. Low frequency; jump is idempotent (re-focus same pane); rotation
  mitigated by `configChanges`. Optional hardening for a successor change.
  — reviewer A, round 1.
