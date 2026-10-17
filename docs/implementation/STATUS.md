> **Working repository:** https://github.com/vikaskunjbiharishah-ui/GUD.git — branch `main` only. `shahvikask/kosha` is the frozen original reference, not the repository to commit to.

# GrowUrDhan Native Redevelopment — Status (GUD main)

**Initialized:** 2026-10-16. **Repository:** https://github.com/vikaskunjbiharishah-ui/GUD , branch **`main`**. **Pre-documentation HEAD reviewed:** `33c3ffb43b62baa969184f67dfbef67d0d4354d4`. Documentation-only commit SHA must be verified live after publication; a document cannot safely embed its own future commit SHA.

**Authoritative instructions:** [AGENTS.md](../../AGENTS.md) · [Full specification v1.2](../GROK-FULL-NATIVE-REDEVELOPMENT-SPEC.md) · [Session handoff](SESSION-HANDOFF.md) · [Decisions](DECISION-LOG.md) · [CI history](CI-RUNS.md). Source evidence: [M27 checkpoint](../native-migration/27-M27-HOLDINGS-LEDGER-IMPLEMENTATION-CHECKPOINT.md), [master issue #1](https://github.com/vikaskunjbiharishah-ui/GUD/issues/1). **Do not read Grok's independent development history into this status.**

## Milestone ledger — do not conflate source, CI and physical acceptance

| Milestone | Source assessment based on committed checkpoints | Current acceptance / next gate |
|---|---|---|
| M00–M23 | Existing native scaffolding and earlier feature increments already in this repository | Historical evidence only; further audit by individual issue |
| M24 / #2 | Native schema v4, source-parity contracts, encrypted backup and field protection | **Scoped milestone CLOSED** based on earlier automated evidence. Final handset/security under M34 |
| M25 / #3 | App login/recovery and authenticated legacy restore significantly implemented | **OPEN:** corrected current code not green-tested in combined exact-SHA emulator regression; owner decision on intentional secure UX deviation remains applicable |
| M26 / #4 | Owner/family/accounts/HUF/nominees/goals significantly implemented | **OPEN:** complete combined regression and scoped acceptance required |
| M27 / #5 | Eight categories, 36-key details, rich holding profiles, 10 user event types, correction logic, history UI and tests committed | **OPEN:** exact original form-field/create parity, fund-specific fields, goal picker, app-wide close state, accessibility and **unrun current-source Gradle/CI** |
| M28 / #6 | Some underlying statement/parser/review work predates current milestone | **NOT ACCEPTED**; audit all issuer and history requirements before claiming coverage |
| M29 / #7 | Financial domain engines and limited Home/Insights exist | **NOT ACCEPTED**; exact parity plus full UI/tax/interest/EMI pending |
| M30 / #8 | Official-quote policies exist, not an accepted network integration | **NOT ACCEPTED** |
| M31 / #9 | Partial backup/settings/design exists, G14 visual workflow not yet verified | **NOT ACCEPTED** |
| M32 / #10 | Gmail policy scaffolding; live OAuth/WorkManager not accepted | **NOT ACCEPTED**; owner Cloud setup needed |
| M33 / #11 | Play entitlement policy and release gates; live Play Billing not accepted | **NOT ACCEPTED**; owner Play/legal keys needed |
| M34 / #12 | Final release acceptance | **NOT STARTED / OPEN:** physical-device, actual parser, security, signing, legal/owner gates |

## Requirement trace

- Exactly **39 P requirements** and **18 E requirements** are inventoried in [parity-register.csv](parity-register.csv). Their statuses are intentionally **NOT RE-AUDITED** at this initialization, except where historic scoped evidence is explicitly cited. An inventory row is not acceptance.
- Each feature needs original path, Kotlin source, tests, current exact-SHA evidence, applicable G14 screenshots for UI and owner approval before final acceptance.
- **Active owner-approved objective:** install repository continuity instructions and return a handoff; **next implementation objective proposed:** close outstanding M27 requirements, run inexpensive local validation first, then seek deliberate exact-SHA integration CI authorization. Preserve M25/M26 pending regression in the tracking register; do not silently close them.
- **Last known combined regression:** [Build #18, run 38237260337](https://github.com/vikaskunjbiharishah-ui/GUD/actions/runs/38237260337), tested `3e41b7c4ac04261953beb75e79a57c6bdea3e6a3`, **FAIL** overall (66/66 data tests passed; 57/58 app tests passed). Later test correction and M27 source commits are not covered by that run.
- **Local Gradle/JVM/Android tests for the current M27 HEAD:** **NOT RUN** in the documented checkpoint. **Physical acceptance:** deferred to M34. **Provider integration:** pending Cloud/Play prerequisites.
- **GitHub Actions:** documentation initialization does not authorize a paid run. Existing workflows remain authoritative until any proposed new G00–G14 gate is actually implemented.


## M27 continued source checkpoint — 2026-10-16

Native `main` now contains an **unverified source-only M27 expansion**: atomic initial details+owner goal registration, eight persisted original instrument fields in encrypted metadata, protected existing-holding goal linking, exact account filtering, archive/closed visibility across Home/Portfolio lists and basic new UI/input validation. Details: [M27 source extension](../native-migration/27-M27-SOURCE-EXTENSION-2026-10-16.md). Unit/Compose/SQLCipher tests have been authored but not executed; **M27 #5 remains OPEN** and neither Kotlin compilation nor emulator safety can be inferred. Pending visual G14 and original specialist-conditional requirements are documented there. Source commit: **resolve current `main` HEAD after this publication**.

**M25 / M26 remain OPEN**. Build #18 tested older source `3e41b7c...` and was red. Its test correction and this M27 work await one deliberate owner-authorized combined exact-SHA workflow. No paid CI, production AAB, Google/Play integration, security signoff or real-device evidence was generated by this source commit.


## 2026-10-16 M27 original-form and opening-debt continuation
See [M27 specialist form / debt checkpoint](../native-migration/27-M27-FORM-AND-OPENING-DEBT-CHECKPOINT.md). Additional source and 10 authored synthetic JVM/Android tests extend funded NPS/FD/AIF/SGB/MF/loan forms, verified opening debt and safe closing; **M27 OPEN, NOT ANDROID-COMPILED OR CI-ACCEPTED**. M25/M26 OPEN. Isolated policy compilation with six stubbed assertions passed only; expensive GitHub Actions not authorized or dispatched. Two-stage original opening UX D-006 proposed, not owner accepted.


## 2026-10-16 M27 source safety/cached security picker checkpoint

The newest main-only source increment adds typed offline cached official-security candidate search (real authenticated NSE/BSE/AMFI loader NOT connected, remains M30), exact created-holding-ID into independently device-reviewed OPENING_BALANCE, prevents account-only Saved routes, prevents same-owner duplicate ISIN, resets drafts on trade-kind/holding changes, improves event type allowlists and adds backup metadata tests. See [audit](../native-migration/27-M27-SECURITY-IDENTITY-PARITY-AUDIT.md). 12 authored tests; source lexical checks passed; isolated exact-blob Kotlin catalog test passed seven assertions with model stub only. Android compilation/Gradle/emulator/CI all NOT RUN. M27 remains OPEN, M25/M26 OPEN. D-006 parity deviation NOT APPROVED. No GitHub Actions authorization.


## 2026-10-16 M27 — AMFI public market-data gateway and financial INR entry

Source increment from 74178c356ff72524ba9791b457e1f29960abf298 adds opt-in HTTPS AMFI NAV refresh with bounded strict parser, app-private atomic cache, offline picker injection, and exact rupee-to-integer minor-string conversions for original amount details. Five source tests authored, **not executed**. The official old AMFI text endpoint may be discontinued, real current format not verified; NSE/BSE adapters not implemented. See [checkpoint](../native-migration/27-M27-AMFI-MARKET-AND-FINANCIAL-INPUT-CHECKPOINT.md). **M27 OPEN (P09–P15), M25 OPEN, M26 OPEN.** D-006 remains owner-PROPOSED. No paid Actions, Android Gradle, emulator or device tests run.
## Repository correction — 2026-10-17

The owner said `shahvikask/kosha` was the wrong working repository. Instructions in this repository now name https://github.com/vikaskunjbiharishah-ui/GUD.git branch `main`. Milestone acceptance is unchanged.

## 2026-10-17 M27 local AMFI JVM tests and Android-test compile

Production AMFI parser, catalog gateway, and INR field editor were reviewed and left unchanged. `:domain:compileKotlin` and `:app:compileDebugKotlin` succeeded. Local Gradle `:domain:test` ran `OfficialAmfiNavParserTest` (3), `OfficialQuotePolicyTest` (3), and `LocalOfficialSecurityCatalogTest` (4): **10 tests, 0 failures**. That does not prove a live AMFI download or an emulator. `:app:compileDebugAndroidTestKotlin` failed on the removed Compose `assertDoesNotExist` API and a missing `CustodyHoldingScreen` import; the test sources were updated to `onAllNodesWithText` / `onAllNodesWithTag` plus `assertCountEquals(0)`, then recompiled successfully. Instrumented tests were **NOT RUN**. **M27, M25, and M26 remain OPEN and NOT ACCEPTED.** No GitHub Actions run was authorized or dispatched.
