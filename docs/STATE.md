# STATE

## HEADER
- Date: 2026-10-28 20:26–20:55 UTC. Run 2. Code head `3b9e8c0` (docs commit follows).
- JVM this run: `FirstRunRegistrationTest` 3/3. Run 1 domain 253/253 and app unit 110/110 not repeated.
- Emulator `gud_x86` API 35 aosp_atd, adb instrument (Gradle was not running): app 77/77, data 81/82.
- ARM64 debug APK rebuilt. Real PDF parse and phone install not run.

## MATRIX
- Q0: LOCAL from run 1. Full instrumented suites now EMULATOR except one data failure below.
- P01 register: EMULATOR `FirstRunIdentityShellTest`, `FirstRunIdentityInstrumentedTest`. LOCAL `FirstRunRegistrationTest`. Activity path that writes identity after the device credential is wired in `MainActivity` and not instrumented.
- P02 login/lock/180-day/offline recovery: EMULATOR `LocalLoginShellTest` 5/5, `LocalAppPasswordInstrumentedTest` 7/7 (includes overdue rotation). Copy only; the rules already existed.
- P15 owner lens: EMULATOR `OwnerLensShellTest` (Insights and Manual trade only).
- P09 eight categories: EMULATOR `CustodyHoldingShellTest.newHoldingOffersAllEightCanonicalCategories` (menu only).
- Same-owner ISIN: EMULATOR `duplicateIsinForSameOwnerCannotCreateSecondFinancialHolding`.
- App classes EMULATOR 77/77: NativeCasIdentityBridge 5, PydanticCoreImport 3, CustodyHoldingShell 13, EmergencyExportShell 2, FamilyOnboardingShell 4, FirstRunIdentityShell 1, LegacyBackupShell 4, LocalAppPassword 7, LocalLoginShell 5, ManualTradeShell 6, NativeShell 16, OfficialAmfiCatalogGateway 1, OwnerLensShell 2, PortableRecoveryShell 2, PortfolioInsightsShell 6.
- Data classes EMULATOR: FamilyAdministration 8/8, FirstRunIdentity 1/1, LegacyBackupInspector 3/3, LegacyVaultMigration 10/10, NativeAuthorization 3/3, NativeCipherIntegrity 1/1, NativeCustodyHolding 20/21, NativeFamilyOnboarding 4/4, NativeFinancialPersistence 5/5, NativeManualTrade 6/6, NativeMutationAuthorization 2/2, NativeParitySchema 5/5, NativeStatementApproval 6/6, NativeVault 7/7.
- Evidence: `evidence/2026-10-28-2052/junit-summary.txt`.
- BLOCKED-OWNER: real Gmail consent, Play purchases, production signing, phone install, real CAS PDFs, pen test, owner visual sign-off. DEVICE/OWNER none.

## QUEUE
1. Q3 per-category create → restart → negative (menu is the only proof).
2. Q2 owner lens on the screens that still show the whole household.
3. Q1 schema: `ledger_profile` still lacks settlement_date, unit_balance_after, description, entered_by.
4. Fix or replace `allOriginalHoldingDetailKeysPersistWithTheirHistoricalValueTypes` without accepting illegal enums.
5. Instrument the first-run activity path (device credential, then identity).
6. Q4 NPS/CDSL staged import with synthetic contracts.
7. Q5 Insights drill-down is still read-only outside the owner filter.
8. Q6 NSE/BSE fetch beside AMFI.
9. Q7 Hindi onboarding and 200% font screenshots.
10. Q8–Q9 fake Gmail gateway and `gud_annual` trial clock.

## DECISIONS
- Parser native libraries are inside `assets/chaquopy/*.imy` (pydantic_core and libpdfium in `requirements-common.imy`, CPython modules in `stdlib-arm64-v8a.imy`), not `lib/arm64-v8a`. That directory has Python, Chaquopy, and SQLCipher only.
- PAN stays on first-run because `legacy_account_identity` requires it. Consent is `registration_consent` inside `extra_json`. Default `registrationConsent=true` keeps older call sites compiling; SQL still rejects false.
- Dashboard revision tripwire stays 6 (run 1). Licence stays one yearly `gud_annual`.
- `allOriginalHoldingDetailKeys…` writes `"synthetic"` into enumerated fields. Combined NSDL+CDSL demat and `linkedHoldingId` on SPECIALISED are illegal. Validation was not loosened.
- Shell tests follow `MoneyEngine.formatRupees`. Amounts were not changed. Loan guidance text is the sentence already on the screen.

## BLOCKERS
- Host RAM: assemble with the emulator killed, then test with Gradle stopped. The emulator raises `-memory 2048` to 2560 MB.
- `pypdfium2` is arm64-only. x86 CAS tests stay synthetic.

## KNOWN GAPS
- One data test still fails (illegal fixture). First-run device-credential handoff is not emulator-proven.
- 360dp / 200% / dark screenshot matrix not saved.
- ARM64 parser behaviour is DEVICE: pending owner.
