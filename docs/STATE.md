# STATE

## HEADER
- Date: 2026-10-29 08:04–09:02 UTC. Run 4. Code head `cbee32e` (docs commit follows).
- Emulator `gud_x86` API 35 at 1080×1920 / 480 dpi (360×640 dp). Gradle was stopped before `adb instrument`.
- Clean full run before the 48dp change: data 93/93, app 86/86. After that change, only the touched UI classes were re-run (not the whole app suite).
- Domain JVM: touched classes only, not the whole `:domain` suite.
- ARM64 APK rebuilt from `cbee32e`. Phone not used.

## MATRIX
- P04 ledger profile: EMULATOR `v4LedgerProfileGainsLegacyColumnsWithoutRewritingEvents` inside `NativeParitySchemaInstrumentedTest` 6/6. v4 rows keep null settlement, balance, and description, and entered_by `owner`. A settlement before the trade date is rejected. Close/reopen keeps the row. An old short format-4 backup still restores. `validate_schema.py` PASS.
- Q4: EMULATOR `SyntheticIssuerImportInstrumentedTest` 1/1. Synthetic CDSL and NPS text: parse, review, one opening each, second commit `ALREADY_IMPORTED`, second prepare rejected, wrong owner rejected before a write. CDSL cost is the printed ₹800, not the ₹100 price. Real PDFs stay BLOCKED-OWNER. NSDL enrolled import is still rejected.
- Q5: LOCAL `InsightsDrilldownTest`. EMULATOR `PortfolioInsightsShellTest` 6/6 and `OwnerLensShellTest` 8/8, re-run after the 48dp buttons.
- Q6: LOCAL `OfficialBhavcopyParserTest`. EMULATOR `OfficialExchangeCloseGatewayInstrumentedTest` 2/2. Mock NSE zip and BSE CSV, as-of date from the file, offline cache, HTML does not replace the cache.
- Q7: EMULATOR `ScreenshotMatrixInstrumentedTest` 6/6. Captures at 1080×1920 (360×640 dp): onboarding English font 1 light, Hindi font 2 light, licence trial font 1 light and font 2 dark, terms font 2 dark, insights font 2 dark. Buttons asserted at least 48dp. Evidence `evidence/2026-10-29-0900/screens/`. Hindi body at 200% scrolls inside the card. TalkBack not run.
- Q8 profile: LOCAL `GmailProfileTest` (token stays out of the URL; HTML and a missing email are refused). Not in the 86. Real consent stays BLOCKED-OWNER.
- Q9: EMULATOR the same licence shots. Screen says one yearly plan at ₹99 and no lifetime licence. `Licence.SKU_ANNUAL` is still `gud_annual` and is not painted. No Play purchase.
- Evidence for the full suites: `evidence/2026-10-29-0824/junit-summary.txt`.
- BLOCKED-OWNER: real Gmail consent, Play purchases, production signing, phone install, real CAS PDFs, pen test, owner visual sign-off. DEVICE/OWNER none.

## INSTRUMENTED COUNTS
- data 93/93: EightCategoryRestart 9, FamilyAdministration 8, FirstRunIdentity 1, LegacyBackupInspector 3, LegacyVaultMigration 10, NativeAuthorization 3, NativeCipherIntegrity 1, NativeCustodyHolding 21, NativeFamilyOnboarding 4, NativeFinancialPersistence 5, NativeManualTrade 6, NativeMutationAuthorization 2, NativeParitySchema 6, NativeStatementApproval 6, NativeVault 7, SyntheticIssuerImport 1.
- app 86/86 before the 48dp change: FirstRunIdentityHandoff 1, NativeCasIdentityBridge 5, PydanticCoreImport 3, OfficialExchangeCloseGateway 2, CustodyHoldingShell 13, EmergencyExportShell 2, FamilyOnboardingShell 4, FirstRunIdentityShell 1, LegacyBackupShell 4, LocalAppPassword 7, LocalLoginShell 5, ManualTradeShell 6, NativeShell 16, OfficialAmfiCatalogGateway 1, OwnerLensShell 8, PortableRecoveryShell 2, PortfolioInsightsShell 6.
- Re-run after 48dp: ScreenshotMatrix 6, PortfolioInsightsShell 6, OwnerLensShell 8. The other app classes were not repeated.

## QUEUE
1. Q8 fake mailbox sync, WorkManager, quarantine. Profile parse is LOCAL. Real OAuth stays blocked.
2. Q10 release minify.
3. NSDL staged import (CDSL and NPS synthetic path is in).
4. Activity UI test of the credential screen itself.
5. Full `:domain:test` was not re-run this run.
6. Manual price override UI next to the close cache.
7. Full `:app` suite was not repeated after the 48dp change.
8. TalkBack pass on the screenshot screens.
9. Do not treat a missing close as zero.
10. Home and Settings are not in the screenshot matrix yet.

## DECISIONS
- New ledger columns are on `ledger_profile` via migration v5, not a rebuilt `ledger_event`. Fresh installs ALTER after DDL. `entered_by` stays `'owner'` only, matching `src/db/repository.ts`.
- Backup envelope stays format 4. `databaseVersion` 5 is the new export. A version-4 export omits the four new columns and restores them as null / `owner`.
- CDSL matches enrolled ISIN + BO id, not the printed name. NPS matches PRAN + scheme code + scheme name. Email is not an identity.
- NSE/BSE prices are parsed from the official UDiFF close column only. Series GB is `GOLD_SGB`. Empty or HTML files do not invent a price.
- Parser libraries remain inside `assets/chaquopy/*.imy`, not `lib/arm64-v8a`.
- Licence stays one yearly `gud_annual`. The screen says the price, not the SKU.
- Screenshot width is the emulator logical size (1080 px at 480 dpi), not a cropped bitmap.

## BLOCKERS
- Host RAM: assemble with the emulator killed. The emulator raises `-memory 2048` to 2560 MB.
- `pypdfium2` is arm64-only. x86 CAS tests stay synthetic.

## KNOWN GAPS
- Not every screen was shot in both themes. TalkBack was not run.
- ARM64 parser behaviour is DEVICE: pending owner.
- The whole domain JVM suite was not repeated after these slices.
- The full app instrumented suite was not repeated after the 48dp buttons.
