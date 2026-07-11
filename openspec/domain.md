# termux-app (cartwmic fork) Domain

**Version:** 1.0.0
**Last updated:** 2026-07-11

## Entities

- **TerminalSession** — a running pty-backed shell process; receives input bytes via `session.write(...)`; exactly one session is "current" in the activity at a time.
- **TerminalView** — the view rendering the current session's screen; owns terminal key focus and receives hardware/IME key events routed as terminal input.
- **Terminal toolbar** — the bottom UI strip between TerminalView and the soft keyboard, composed of the extra-keys row and the text-input box; presented stacked (fork default, both visible) or paged (legacy upstream ViewPager, one at a time), selected by the `terminal-toolbar-stacked` property.
- **Extra-keys row** — configurable grid of buttons (ESC, TAB, arrows, …) defined by the `extra-keys` property; a tap writes the key's escape sequence directly to the current TerminalSession.
- **Text-input box** — the single-line `EditText` (`terminal_toolbar_text_input`) in the toolbar; IME text is composed here and sent to the current session on the editor send action, then the box is cleared. Both toolbar modes wire it through `TerminalToolbarViewPager.setupTextInputView`.
- **termux.properties** — user-editable property file governing toolbar layout, extra keys, and other behavior; reloaded at runtime via `termux-reload-settings`.
- **App preferences** — app-private stored preferences (e.g. `show_extra_keys`) governing per-device UI state such as toolbar visibility.
- **Notification jump** — the fork's deep-link capability that focuses a specific session/pane from a notification (see `openspec/specs/notification-jump/spec.md`).

## Invariants

1. Text sent from the text-input box is written verbatim to the current TerminalSession; an empty send transmits `\r`.
2. If the current session has exited, the text-input send action removes the finished session instead of writing to it.
3. Extra keys write directly to the current TerminalSession and never move focus away from TerminalView.
4. Shared toolbar components behave identically in stacked and paged modes; they use the same view ids and the same setup helpers.
5. Terminal-input-derived data is never persisted to disk, SharedPreferences, or logs without explicit user opt-in (constitution V); in-memory process-lifetime retention is permitted.
6. Unsent text-input contents survive activity recreation (rotation); in-memory state does not survive process death.
7. Property changes take effect after `termux-reload-settings` without requiring a cold app restart.
8. Toolbar show/hide toggles act on the toolbar as a whole, governed by the `show_extra_keys` preference in both modes.

## Units and conventions

- **Time**: epoch milliseconds in memory (`System.currentTimeMillis()`); user-facing times rendered as relative strings (e.g. "2m ago").
- **Text encoding**: text written to a session is encoded by the terminal-emulator layer (UTF-8 pty stream); the app does not re-encode.
- **IDs/naming**: Android view ids and resources snake_case; `termux.properties` keys kebab-case; Java code camelCase; classes under `com.termux.app.*` (app UI) vs `com.termux.shared.*` (shared library) vs `com.termux.terminal.*` (emulator core).
- **Layout**: toolbar heights derive from `terminal-toolbar-height` scale factor times row units.

## Out-of-scope domains

- **Shell-level history** (`~/.bash_history`, zsh history, readline) — owned by the shells running inside sessions; the app never reads or writes it. App-level text-input history is a distinct, app-owned concept.
- **Terminal emulation semantics** — escape-sequence interpretation belongs to the `terminal-emulator` module; app-layer features treat session I/O as opaque bytes.
- **IME implementation** — the app is a consumer of system soft keyboards, never an input-method provider.
- **Termux plugin ecosystem** (termux-api, termux-styling, package repos) — not modeled by this fork's specs.

## See also

- Constitution: `openspec/constitution.md`
- Schema docs: `~/.local/share/openspec/schemas/opsx-superpowers/README.md`
