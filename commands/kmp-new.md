---
description: Scaffold a new KMP/CMP app (Android + iOS) into the current empty folder, tailored to your org, name, and domain.
argument-hint: "[app name] [org, e.g. com.acme]"
---

Create a new Kotlin Multiplatform + Compose Multiplatform application (Android + iOS) in the current
directory by invoking the **kmp-scaffold** skill.

Arguments (optional, the skill will ask for anything missing): $ARGUMENTS

Follow the skill end to end: confirm the folder is empty, run the short interview (app name, org,
what the app is about, targets, Firebase), generate the project with
`${CLAUDE_PLUGIN_ROOT}/scripts/generate.py`, tailor the feature modules to the user's domain, verify
the build with `${CLAUDE_PLUGIN_ROOT}/scripts/verify.sh`, and finish with clear next steps. Assume the
user may not be a mobile expert — explain in plain terms and lead with sensible defaults.
