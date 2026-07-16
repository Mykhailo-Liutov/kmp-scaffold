# Changelog

## 0.1.1 — 2026-07-16

Hardening from the BrewLog end-to-end smoke review (self-audit + Codex second opinion):

- Feature-author agent: hard data-ownership rule (user-authored local data → migration-safe
  `buildDefault`, destructive builder only for remote caches) with a pre-report self-check;
  behavioral doc-sync step (architecture examples must match real code, complete README module
  graph, sample-backed remote sources flagged).
- Scaffold skill: post-rename sweep is behavioral, not just lexical.
- Template: Fastfile no longer references the Firebase-only stage workflow in
  Firebase-off projects.

## 0.1.0 — 2026-07-15

Initial public release (beta).

- Deterministic generator: golden-template copy with ordered single-pass identity replacement,
  package relocation, Firebase sentinel stripping, Android-only mode.
- Feature ops: transactional `clone` (from the plugin's pristine archetypes) / `remove` /
  `capability` (domain:x + data:x split), with identifier validation (reserved words, charset).
- Identity validation: XML/YAML/Kotlin-safe display names and base URLs, reserved-word package
  segments rejected.
- Generated template: multi-module KMP/CMP clean architecture, convention plugins, Koin, Ktor +
  Room (migration-safe default builder; destructive recreation is an explicit cache-only opt-in),
  Arrow typed errors, Kermit logging, detekt/ktlint, SHA-pinned CI with least-privilege
  permissions, checksum-verified gitleaks scanning, Gradle wrapper checksum.
- Orchestration: interview → generate → tailor (feature-author agent) → verify → document;
  conventions blueprint shipped into each generated repo as `docs/ARCHITECTURE.md` +
  `.claude/rules/`.

Known limitations are tracked in `README.md` (Status / Limitations) and `docs/DEVELOPMENT.md`
(roadmap): distribution lanes are stubs, prod flavor ships a demo endpoint (loudly flagged),
feature `remove` is not build-preserving, no plugin CI matrix yet.
