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
