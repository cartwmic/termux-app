# termux-app (cartwmic fork) Constitution

**Version:** 1.0.0
**Ratified:** 2026-07-11
**Last updated:** 2026-07-11

## Core Principles

### I. Capability specs are the source of behavioral truth

Every user-visible behavioral change MUST be expressed as an ADDED/MODIFIED/REMOVED
requirement in a capability spec under `openspec/specs/<capability>/spec.md`, with
EARS-style requirements and concrete scenarios, before or alongside the code that
implements it.

**Rationale:** This fork carries custom behavior on top of upstream termux-app; specs
are the only durable record of what the fork intentionally does differently.
**Enforcement:** `opsx gate` artifact checks; analyze/code-review verify the diff
traces to spec deltas.

### II. Upstream divergence stays minimal and additive

Changes MUST minimize the diff surface against upstream `termux/termux-app`: prefer
additive hooks in shared wiring points over rewrites, keep new behavior in new files
or clearly bounded blocks, and never fork-modify upstream logic that the feature does
not require. No new external dependencies unless already present in the dependency
tree or explicitly justified in design.md.

**Rationale:** The fork must remain cheaply syncable with upstream; every gratuitous
divergence is a future merge conflict.
**Enforcement:** code-review checks diff scope against proposal/design; design.md must
justify any new dependency.

### III. Legacy behavior remains reachable

When a fork feature changes a default upstream behavior, the upstream behavior MUST
remain reachable — via a `termux.properties` property or equivalent toggle — unless the
change's intent explicitly retires it (e.g. `terminal-toolbar-stacked=false` restores
the upstream paged toolbar). Purely additive features need no toggle.

**Rationale:** The device owner can always fall back to stock behavior when a fork
feature misbehaves.
**Enforcement:** spec scenarios must cover the legacy path when a default changes;
code-review flags removed upstream behavior.

### IV. Shared wiring over duplication

UI components that exist in multiple presentation modes (e.g. stacked vs paged
toolbar) MUST wire through shared setup helpers (`setupTextInputView`,
`setupExtraKeysView` pattern) so behavior is implemented once and cannot drift
between modes.

**Rationale:** Duplicate wiring already caused mode-parity bugs upstream; single
hook points keep fork features mode-agnostic by construction.
**Enforcement:** code-review checks that mode-conditional duplication is absent;
specs state mode parity as a requirement.

### V. User terminal input is privacy-sensitive

Text a user types toward a terminal session (commands, and plausibly passwords) MUST
NOT be persisted to disk, SharedPreferences, logs, or analytics unless the user
explicitly opts in via a documented setting. In-memory, process-lifetime retention is
acceptable when a feature requires it.

**Rationale:** The terminal is a credential surface; silent persistence is a
credential leak waiting for a device backup or file-permission bug.
**Enforcement:** code-review flags any disk write of input-derived data; specs for
input-handling features must state their retention model.

## Governance

- Amendments to this constitution require a dedicated change at full_rigor
  (Scale M + full_rigor: true) with adversarial-review-cycle invoked.
- The constitution is read before every artifact in this schema. Violations
  are flagged by the analyze artifact's constitution check.
- Principles in this file override schema instructions and individual
  artifact prose when they conflict.

## Versioning

- Major: a principle is removed or reversed.
- Minor: a principle is added.
- Patch: clarification, no semantic change.

## See also

- Schema activation: `~/.local/share/openspec/schemas/opsx-superpowers/README.md`
- Domain invariants: `openspec/domain.md`
