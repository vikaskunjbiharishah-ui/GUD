# GUD main — Actual GitHub Actions and Grok G00–G14 gate crosswalk

**State on 2026-10-16:** Existing workflows were inspected by name; **G00–G14 proposed workflow filenames from specification §10 were NOT installed by this documentation checkpoint.** This document maps the intended verification outcomes to current automation and future gaps; mapping is **NOT** proof a job exists or passes. Owner controls paid CI.

| Contract gate | Current or prospective mechanism | Honest status on this checkpoint |
|---|---|---|
| G00 frozen source | Native Python security/schema preflights / `native-android.yml` plus parser hashes | Partially present; exact hash/coverage audit pending |
| G01 static / compile | `native-android.yml` and existing local Gradle/Python tools | Existing partial coverage; Detekt/Lint audit needed |
| G02 unit | Native Gradle JVM suites, source TS tests in `ci.yml` | Existing tests, no green full CI on current M27 SHA |
| G03 finance golden | Native domain golden tests / TS fixtures | Partial; full Kotlin vs TS source parity pending |
| G04 encrypted vault | Native SQLCipher Android instrumentation | Existing tests, no current-SHA M27 acceptance |
| G05 debug APK and minified AAB | `native-android.yml` | Exists historically; run exact SHA only with approval |
| G06 Compose emulator | `native-android.yml` | Exists; historical Build #18 app sub-suite failure |
| G07 frozen CASPARSER | Parser boundary/source preflights and synthetic tests | Real ARM64 PDF parsing deferred to M34 |
| G08 mobile AppSec and supply chain | Existing security preflights and `native-signed-aab.yml` | Partial; independent MASVS/SBOM not signed off |
| G09 market/Gmail/Play adapters | Kotlin policy tests; future real provider contract tests | External service integrations incomplete, no pass claim |
| G10 Android OS matrix | `native-device-matrix.yml` | Exists; source-specific run unverified |
| G11 exact-SHA orchestrator | `native-android.yml` manually pinned source SHA | Existing analog, but strict full G00–G14 aggregator not established |
| G12 production signer | `native-signed-aab.yml` protected | Owner-approved release only; **NOT RUN** here |
| G13 quality report | Current JUnit/run artifacts; proposed unified evidence manifest | **GAP**: single-source machine-readable G00–G14 dashboard |
| G14 premium Material 3 design QA | No independently verified dedicated visual/screenshot accessibility gate | **GAP**: screenshot, 200% font, contrast, TalkBack and owner signoff |

## Actual source workflow catalog

` .github/workflows/native-android.yml ` — manually invoked native Kotlin/SQLCipher/Compose regression and smoke AAB.

` .github/workflows/native-device-matrix.yml ` — Android emulator compatibility matrix, owner-run as required.

` .github/workflows/native-signed-aab.yml ` — protected versioned/signer-controlled Play bundle step; never confuse disposable-CI smoke signing with production acceptance.

Legacy/repository workflows `ci.yml`, `android-aab.yml`, `android-apk.yml`, `android-enhancements.yml`, `marketing.yml` are present. Do not assume they constitute native G00–G14 until steps and triggering rules are actually inspected.

## Workflow implementation/change policy

1. Audit real YAML, triggers, test modules and artifact URLs before changing CI architecture. Protect manual dispatch and budget, retain old React/Tauri golden source oracle until Kotlin parity.
2. Build a **job-level named matrix** of G00–G14, using existing workflows/reusable jobs to avoid redundant emulator runs. Proposed `g00-...` files are optional if equivalent actual job labels are verifiable.
3. For every owner-authorized CI dispatch: verify full 40-hex source SHA, checkout exactly that SHA, run selected required gates, show **PASS/FAIL/NOT RUN/BLOCKED** per gate, keep redacted JUnit, screenshots and artifact fingerprints; after correction rerun **new SHA**. No skipped stage silently passes.
4. G14 must run on UI-bearing milestones once implemented, with accurate light/dark/small/large/font200/landscape screenshot and accessibility evidence; cannot fabricate owner design acceptance.
5. G12 requires owner credentials, exact approved SHA, external signing proofs and separate manual approval. No release upload without explicit instruction.
6. Do not add or launch new paid Actions in this documentation-only checkpoint.


## Proposed M25+M26+M27 integration checkpoint (2026-10-16)

Existing `native-android.yml` was inspected: owner can use **workflow_dispatch** with `source_sha` full 40-character SHA; `main` ordinary commits do not trigger CI. It already runs `validate_schema.py`, Python unittest security regression, `security_preflight.py`, JVM `:domain:test :app:testDebugUnitTest`, debug/test APKs, disposable-key minified release bundle, and API35 `:data:connectedDebugAndroidTest :app:connectedDebugAndroidTest`. Use it for **one combined checkpoint after source completion**, monitor terminal jobs and reports and correct failures on `main`; no current CI PASS. The separate `native-device-matrix.yml` covers 26/30/36 but requires manual approval. G14 visual/large-font/accessibility gate is still not a confirmed dedicated CI job.


## 2026-10-16 acceptance checklist
Detailed owner-controlled combined run plan: [M25-M26-M27-EXACT-SHA-REGRESSION-CHECKLIST.md](M25-M26-M27-EXACT-SHA-REGRESSION-CHECKLIST.md). No new G00–G14 Actions YAML was created. No paid Actions run triggered. Authenticated official NSE/BSE/AMFI cache M30 and G14 visual QA remain unverified.
