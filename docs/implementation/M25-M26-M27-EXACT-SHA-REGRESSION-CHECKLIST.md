# Proposed Combined Exact-SHA M25 + M26 + M27 Regression Checklist
Status: NOT AUTHORIZED / NOT DISPATCHED. Starting main SHA: 3ef250021d44170e8f2b7c7e23f153a76b5016a2; verify newest main HEAD immediately before workflow dispatch. Never substitute mutable branch for tested 40-character source SHA.

## Existing real workflow
- Native GitHub Actions YAML: .github/workflows/native-android.yml ("GrowUrDhan Native Android M3"). Manual workflow_dispatch source_sha set to current 40-digit SHA; normal main commits do not trigger. Do NOT alter .github/ci-trigger/native-regression.txt or run paid CI without owner permission.
- Gate A: Python schema migration, unittest security regression and security_preflight; frozen CASPARSER source presence.
- Gate B: Gradle :domain:test :app:testDebugUnitTest; exact golden money and Kotlin ViewModel tests; upload JVM JUnit XML and HTML.
- Gate C: :app:assembleDebug :app:assembleDebugAndroidTest :data:assembleDebugAndroidTest; downloadable debug and test APKs.
- Gate D: disposable-key :app:bundleRelease, jarsigner verify; NOT production signing or Play release approval.
- Gate E: Android 35 Google APIs x86_64 :data:connectedDebugAndroidTest :app:connectedDebugAndroidTest; uploaded instrumentation JUnit evidence. Physical phones deferred M34.
- Optional API26/30/36 matrix via separate workflow only with separate owner authorization. G14 visual screenshots/a11y workflow not verified as existing.

## Required acceptance groups (all NOT RUN on current SHA)
M25: first install two matching passwords, background/device lock, rotation, recovery codes/questions and legacy KOSHA1/GUDVLT01 locked backup, tamper rejection, key redaction, empty-vault restore.
M26: self/spouse/HUF and guardian/nominee owner selection in Home, Accounts, Holdings, Tax, Documents; member switching, demat/custodian and goal ownership, cross-owner rejection, shared-ownership policy and deletion protection.
M27: exact original eight categories/subcategories, 36 detail fields and full instrument extras including portable backup, NPS/PPF/SSY/EPF/FD/SGB/AIF/REIT/InvIT/property/loans, subtype validation, local catalog latest/stale/future and verified source, owner duplicate ISIN rollback, goal link, new holding preselection and account-only route protection.
M27 financial: BUY/SELL/DIVIDEND/INTEREST/BONUS/REPAYMENT/SPLIT/CORRECTION/OPENING_BALANCE/VALUATION_UPDATE full allowed-kind matrix and disallowed categories; source-only SNAPSHOT_ADJUSTMENT; FIFO negative/backdated reject, correction supersession, scale/fees and stale-book invalidation, loan open/payoff/close and corrected debt; fresh device lock, owner isolation, encrypted backup/reopen and legacy migration.
M27 visual: light/dark premium Material 3, TalkBack, 200% font, accessible touch targets, empty/error state, tablet/landscape, screenshot diffs. G14 currently separate unimplemented full gate; do not count Android unit tests as UX acceptance.
Owner acceptance: D-006 native two-stage initial balance vs original one-submit remains decision pending; official downloader integration belongs to M30. Real ARM64 parser/signing/AppSec under M34.

## Run -> failure -> corrected SHA process
1. Read AGENTS, full v1.2 spec, STATUS, HANDOFF, CI-RUNS, DECISION-LOG; verify GitHub main SHA and owner run authorization.
2. Manually dispatch native-android.yml with input source_sha = newest main SHA. Monitor until each required job ends; capture full tested SHA log line, actual run URL, JUnit XML, screenshot and artifact pointers.
3. A failed/skipped/queued job is NOT PASS. Diagnose stacktrace, create focused correction on main, locally retest, commit and request or use explicit approved CI re-run budget. Test NEW SHA, keep original FAIL in CI-RUNS.
4. Record per-gate PASS/FAIL/BLOCKED/NOT RUN, exact tested SHA, owner decision references, and next prompt in durable handoff. Never claim background monitoring or physical hardware from emulator.


## AMFI / financial-minor extension (2026-10-16; all NOT RUN)

- P10/P26: Exact-sourced AMFI parser JVM tests (valid scheme, negative/future/stale/N.A./HTML/invalid ISIN), explicit user refresh privacy test, bounded 8-MB HTTPS-only URL, no redirects and error preserves last cached data. Check AMFI new format compatibility, record actual source date and endpoint response; do not assume obsolete NAVAll.txt works.
- P10/P26: App instrumentation offline app-private cache load; zero outbound requests when viewing the Holdings screen; no PAN/email/folio/holding identifiers in network requests. NSE/BSE adapters remain missing and must not be marked PASS.
- P11/P12: AIF commitment/annual contributions/property monthly income shown in INR, converted exactly to scaled integer strings, preserved across SQLCipher encrypt/reopen and portable backup; reject overflow/overprecision and verify old stored minor strings display correctly.
- P13/P14: Holding opening balances still require separately reviewed event and fresh OS device credential; D-006 remains owner decision pending.
- Only after source completion and **explicit user authorization**, manually dispatch existing native-android.yml with source_sha equal to newest exact main SHA. Monitor terminal jobs and reports. No Actions in this source session.
