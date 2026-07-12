---
# Machine-readable mode block — the SOLE source opsx gate reads (it never parses
# the prose table below). Keep the table in sync as the human-facing mirror.
scale: M
full_rigor: false
execution_mode: standard
verification_mode: retained-recommended
debug_mode: standard
review_status: not-requested
delegation_mode: single-agent
# code_review_mode: derived when absent — Scale M ⇒ gating-required (fail-closed)
loop_max_iterations: 40
validation_source_mode: required
spec_level: spec-anchored
doneness_mode: required
---

# Review

## Modes

| Mode | Value | Notes |
|---|---|---|
| Scale | M | XS\|S\|M — skills author per Scale (graph is static; gating lives in the skills + opsx gate). Out-of-range fails closed |
| full_rigor | false | false\|true — true opts Scale-M into the former L/XL extras (standalone clarify+analyze, independent doneness judge, ADR promotion, adversarial-on-analyze, retrospective) |
| Execution Mode | standard | standard\|tdd-preferred\|tdd-required |
| Verification Mode | retained-recommended | inline-only\|retained-recommended\|retained-required — retained-required forces verify.md green before archive |
| Debug Mode | standard | standard\|systematic-debugging |
| Review Status | not-requested | not-requested\|requested\|findings-received\|resolved |
| Delegation Mode | single-agent | single-agent\|subagent-eligible\|subagent-required — dispatch via the subagent-dispatch capability hook (pi-subagents is the pi adapter) |
| Code Review Mode | derived (absent) | none\|advisory\|gating-required — default DERIVED when absent: M ⇒ gating-required (fail-closed); an explicit value always wins; gating-required blocks archive on code-review.md Verdict |
| Loop Max Iterations | 40 | iteration budget; mapped onto the loop runtime turn budget. Authoring-time default for Scale M |
| Validation Source Mode | required | required\|waived — waived (with rationale) lets Scale ≥ M pass with no agent-independent validation source |
| Doneness Mode | required | required\|waived — default required at Scale ≥ M; a `waived` value needs a non-empty `doneness_waiver_rationale` (bare waiver fails). Gate reads a sealed `doneness.md` verdict (see templates/doneness.md) |
| Spec Level | spec-anchored | spec-anchored\|spec-first\|spec-as-source (warning if last) |
| Model Config | (unset) | optional `author_model`/`review_models`/`impl_model`/`author_in_session` + `provider`/`*_provider` front-matter keys, resolved by `opsx models`; unset ⇒ session model |

## Diff Base + Worktree locator

**Diff Base SHA:** 70a67e65e8ec7eb2b6685bdde2e5cdc1789c18e7
**Worktree Path:** /Volumes/Workshop/git/termux-app--opsx-add-text-input-history
**Integration Branch:** master

## Manual Adjustments

- Scale M (no full_rigor): typical feature — cross-file (history store + toolbar wiring + bottom-sheet UI) but single capability (`terminal-toolbar` delta). Matches the schema README M heuristic; recommended during the explore session and encoded in the frozen intent.
- Loop Max Iterations 40: the authoring-time default for Scale M (template ships the S default 20).
- All other modes are template defaults; Code Review Mode left absent so the gate derives `gating-required` at M fail-closed.

## Execution Notes

<!-- Transient observations appended during apply. One-line entries when a
non-trivial decision is made mid-task. Durable knowledge → retrospective.md. -->

- 2026-07-11 — worktree ensured via `opsx worktree ensure`; locator captured (Diff Base = master merge-base at creation).

## Scope Expansions

<!-- Evidence-gated widenings (opsx-adversarial-review). One entry per widening;
every entry is surfaced to the user at the decision-audit landing or gate-green.
Out-of-scope findings NOT required for the intent route to follow-ups.md. -->

## Fidelity Round Ledger

<!-- Append-only orchestrator bookkeeping. One row per sealed design-fidelity
judgment round AND per human-waiver ruling. Never remove or rewrite a prior row. -->

| Round | Fidelity | Per-judge verdicts | Attested HEAD |
|---|---|---|---|
