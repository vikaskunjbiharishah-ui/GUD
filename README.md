> **Working repository:** https://github.com/vikaskunjbiharishah-ui/GUD.git — branch `main` only. `shahvikask/kosha` is the frozen original reference, not the repository to commit to.

# GrowUrDhan — Isolated Android Development Mainline

This private repository is the **independent development copy** at
`vikaskunjbiharishah-ui/GUD`. All future changes are made to **`main` only**.
It was seeded from `shahvikask/kosha`, branch `GUD-Android-ChatGPT`,
commit `d98f0945a924049c67cf376b8cc6d78a171dc3d5`.
The original repositories and all of their protected branches are
separate; do not push changes back to them.

## What is here

- **Current native Android migration:** `android-native/` — Kotlin,
  Jetpack Compose, SQLCipher, Keystore-wrapped database keys, family
  onboarding, strict manual CAS review, append-only money ledger,
  locally encrypted portable backups, portfolio and read-only insights.
- **Original reference application:** `src/`, `src-tauri/` and
  `package.json` — the React/Tauri codebase retained as an as-built
  functional reference, not overwritten or automatically migrated.
- **Frozen statement parser:** `src/casparser-main/` and its invocation
  scripts. Do **not** edit or replace this code.
- **Engineering and verification documents:** `docs/`, including
  [the native architecture](docs/native-migration/19-ENTERPRISE-REDEVELOPMENT-MASTER-PLAN.md)
  and [feature parity register](docs/native-migration/20-ENTERPRISE-FEATURE-PARITY-MATRIX.md).

Application ID: `in.growurdhan.app`. The native version is a development
build; it is **not yet accepted for public Google Play distribution**.

## One development commit, one deliberate build

**Normal changes to `main` do not start paid GitHub Actions builds.**
Development work is staged and reviewed as a complete milestone, then pushed
with one final commit. Build and regression testing is a separate owner-approved
gate.

To start the full native pipeline, use **Actions → GrowUrDhan Native Android M3 →
Run workflow → main**, or create/update *only* the explicit file
`.github/ci-trigger/native-regression.txt` in a separate follow-up commit.
That single trigger runs native schema and security preflights, JVM tests,
debug APK/test APK builds, disposable-key release AAB smoke compilation and
Android API 35 instrumentation. The API 26/30/36 emulator matrix and
React/Tauri JS CI are also manual workflows.

The manual production-signing workflow remains protected by separately
approved exact commit SHA, `play-release` environment and owner-provided
signing secrets. There is no automated Play publication.

Run locally with the prerequisites from
[android-native/README.md](android-native/README.md). Passing historic CI runs
from the old account does not verify this repository's newer commits.

## Commercial acceptance remains open

The new Portfolio and Insights pages are read-only, not substitutes for
app-password login parity, full transaction editing, Gmail OAuth, NPS import,
official price refresh, Google Play billing, independent AppSec audit or
physical-device signoff. Nothing in this repository should be called
commercially ready until those gates have evidence.

## Current consolidated development increment

The new native Portfolio flow includes manually reviewed BUY/SELL entry
through fresh Android device credential approval, with final lot/book
revalidation and append-only SQLCipher insertion under one transaction.
Offline Gmail attachment staging, dated official market quotes and Play
purchase-validation contracts are also present, but **their provider
integrations are not enabled**. Production AAB signing is blocked pending
owner-approved SHA-bound release evidence and independent signoff.

See [Milestone 22](docs/native-migration/22-E2-E4-MANUAL-TRADE-SECURE-STAGING-RELEASE-GATE.md).
The next step is the **separate deliberate GitHub build and regression run**.

## Offline features independent of Google Cloud and Play Billing

The next integrated native milestone adds GrowUrDhan-specific app password
admission and password change, locally tracked NPS/PPF/EPF and other
zero-unit assets, read-only inspection of original locked `.kosha`
backups, and a consented plaintext emergency summary export. It does
**not** implement Google OAuth, Play Billing, real NPS statement imports,
or lossless legacy migration. See
[Milestone 23](docs/native-migration/23-OFFLINE-COMMERCIAL-READINESS.md).

This source is not yet Gradle/device verified and is not a public release.
