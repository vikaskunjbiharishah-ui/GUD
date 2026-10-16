# Native redevelopment — immutable GitHub Actions evidence log

This file records **observed historical run results**. The original issues and run logs are the source of truth for older runs; this initial record is **not** an exhaustive reconstruction of all historical CI. Append rows; never revise a failed conclusion to PASS after a fix. Always distinguish *trigger commit*, *tested source SHA*, *new fix commit*.

| Recorded date | Workflow / run | Run URL | Tested source SHA | Observed result | Failure and next evidence |
|---|---|---|---|---|---|
| 2026-10-11 | Native combined M25+M26 Build #14 / 38130131709 | https://github.com/vikaskunjbiharishah-ui/GUD/actions/runs/38130131709 | `d09f5350709ec635788d10f9c9c939a4ab570fe7` | **FAIL** | Data Kotlin nullable smart cast compile failure; later correction `6a8d36109c92afde47b2e4f337e47d6d37c7e8ab` |
| 2026-10-11 | Native combined M25+M26 Build #15 / 38134761299 | https://github.com/vikaskunjbiharishah-ui/GUD/actions/runs/38134761299 | `6a8d36109c92afde47b2e4f337e47d6d37c7e8ab` | **FAIL** | Data test invalid DEMAT type and app UI duplicated member-text selector; corrected in subsequent main source |
| 2026-10-12 | Native M25+M26 Build #16 / 38233265840 | https://github.com/vikaskunjbiharishah-ui/GUD/actions/runs/38233265840 | `76314b1a2a1a8ab2d79911b3a1a3f19ec8975982` | **FAIL** | `hashEmail` unresolved reference; corrected in `031333f3c8b8b80f6afc538f3c563b909acd12bb` |
| 2026-10-12 | Native M25+M26 Build #17 / 38233731585 | https://github.com/vikaskunjbiharishah-ui/GUD/actions/runs/38233731585 | `031333f3c8b8b80f6afc538f3c563b909acd12bb` | **FAIL** | Two emulator test fixture/callback issues; corrected in later source |
| 2026-10-12 | Native M25+M26 Build #18 / 38237260337 | https://github.com/vikaskunjbiharishah-ui/GUD/actions/runs/38237260337 | `3e41b7c4ac04261953beb75e79a57c6bdea3e6a3` | **FAIL** | 66/66 data tests passed; 57/58 app tests passed; `LocalLoginShellTest.firstInstallRequiresTwoMatchingPasswordsBeforeDeviceApproval` clicked nested Text rather than Button; corrected source `58d94aec404c1ed14352f29b152ef62a789f72ae`, **not retested here** |

## Current verification ledger

- `main` before continuity commit: `33c3ffb43b62baa969184f67dfbef67d0d4354d4`, M27 source implementation documented but **NOT locally compiled, emulator tested or exact-SHA CI verified** in its checkpoint.
- **No workflow was launched by the 2026-10-16 documentation initialization.** No current-source passing CI is claimed. Do not count successful historic sub-jobs as full Build #18 success.
- Existing workflow filenames: `native-android.yml`, `native-device-matrix.yml`, `native-signed-aab.yml`, plus historical `ci.yml`, `android-aab.yml`, `android-apk.yml`, `android-enhancements.yml`, and `marketing.yml`. Details and requested future G00–G14 gate mapping in [WORKFLOW-GATES.md](WORKFLOW-GATES.md).
- On next authorized run append **run ID, URL, full tested SHA, terminal job states, artifact references, failing tests, fix commit and replacement run**. Running/queued status is `RUNNING—NOT VERIFIED`. Synthetic tests are not physical device acceptance.


## 2026-10-16 — M27 source-only extension; no run

Starting HEAD: `66d1cb7db9b6b0ada2d96e58df85f88854b45839`. This later source/documentation commit adds M27 advanced forms, atomic goal/profile creation, instrument metadata, archive list status and regression tests, **but GitHub Actions NOT DISPATCHED and no Gradle/JVM/Android tests executed**. Exact source SHA for this increment is the GitHub commit containing this note; **tested source SHA = NONE**. Build #18 and all preceding failures remain unchanged above. Next run requires explicit owner authorization and must test that final SHA, not this earlier baseline.


## 2026-10-16 M27 form and loan opening source follow-up
Starting source SHA bf4ecf3f358fad807deb808064c02e27dd2888e1. New-source exact tested SHA: NONE. GitHub Actions: NOT RUN, NOT AUTHORIZED. Isolated local policy rehearsal (stubbed taxonomy) 6 checks PASS; no full Gradle/JUnit/SQLCipher/Compose/Android proof. Retain failed Build #18 history. Next combined M25/M26/M27 run only after owner approval, with exact new main source_sha and terminal job/artifact evidence.


## 2026-10-16 M27 cached-security and opening-handoff source checkpoint

Starting main SHA 3ef250021d44170e8f2b7c7e23f153a76b5016a2. Twelve new JVM/Compose/SQLCipher test methods AUTHORED but NOT EXECUTED. Full Gradle/JUnit/Android/SQLCipher/APK/AAB NOT RUN. GitHub Actions not dispatched; current-source tested SHA NONE. Exact Git blob catalog and official quote policies compiled locally under kotlinc with stub MoneyScaled; seven standalone offline-provenance assertions PASS. This is not an Android app or project regression. Earlier Build #18 FAILURE remains unchanged. See [combined exact-SHA plan](M25-M26-M27-EXACT-SHA-REGRESSION-CHECKLIST.md). Owner authorization required before any paid regression.
