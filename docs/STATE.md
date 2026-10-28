# STATE

## HEADER
- Date: 2026-10-28 11:40–12:00 UTC. Run 1. Head `2253375` until the report commit.
- JVM: domain 253/253, app 110/110. Scripts: validate_schema PASS; `test_*.py` 35/35.
- Emulator: `gud_x86` API 35 aosp_atd (no google_apis image on this machine). NativeShellTest 16/16.
- Not run this hour: full `:data` / `:app` instrumented suites, release minify, real PDF.

## MATRIX
- Q0 baseline: LOCAL (JVM + scripts). EMULATOR only for NativeShellTest.
- P02 credential tripwire: SOURCE `test_async_financial_views_reject_old_device_credential_results`.
- P25 home amounts: EMULATOR `NativeShellTest` (full-width tiles, one-line rupee scroll).
- Frozen parser: SOURCE `FrozenParserHashTests` + `docs/frozen-hashes.txt` (28 files).
- P01–P39 remainder: SOURCE where a screen or engine already exists in `android-native/`; not re-proved this run. DEVICE/OWNER none.
- BLOCKED-OWNER: real Gmail consent, Play purchases, production signing, phone install, real CAS PDFs, pen test, owner visual sign-off.

## QUEUE
1. Q1 finish: first-run register still splits name, DOB, and security questions onto Family administration after the password.
2. Q1 schema: `ledger_profile` has no settlement_date, unit_balance_after, description, entered_by from `src/db/schema.ts` (not on the TS `LedgerEvent` type).
3. Q2 owner lens on every screen, not only Home.
4. Q3 per-category create → restart → negative on the emulator.
5. Q4 NPS/CDSL staged import with synthetic contracts.
6. Q5 Insights drill-down still read-only.
7. Q6 NSE/BSE fetch beside the AMFI parser (policy enum exists; no bhavcopy decoder).
8. Q7 Hindi onboarding completeness and 200% font screenshots.
9. Q8 Gmail WorkManager behind a fake gateway.
10. Q9 trial clock + fake Billing gateway for `gud_annual`.

## DECISIONS
- Playbook and AGENTS.md point at `docs/PLAYBOOK.md` (owner 2026-10-28). Old native-redevelopment spec stays unread.
- Dashboard revision tripwire is 6, matching `HouseholdDashboardViewModel` call sites, so removing one binding fails.
- Shell tests follow `MoneyEngine.formatRupees` (`₹ 4.50`), not the retired `4.5000 INR` labels. Amounts were not changed.
- Emulator is the existing x86 AVD. Real PDF parse stays DEVICE.

## BLOCKERS
- Host RAM kills the emulator if Gradle runs beside it. Instrumented runs must be after assemble.
- `pypdfium2` is arm64-only. x86 CAS tests stay synthetic.

## KNOWN GAPS
- Full instrumented regression not run this hour.
- 360dp / 200% / dark screenshot matrix not saved.
- ARM64 parser behaviour is not proved here.
