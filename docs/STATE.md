# STATE

## HEADER
- Date: 2026-11-03. Run 11. Code `3fe140c` (also `7fba9c5`). Docs after this line are not the APK commit. Previous code `91f5fe4`.
- Domain ConfirmedFinancePostTest 2/2 at 2026-11-03T00:18:44.595Z. OfficialAmfiNavParserTest 4/4 at 00:29:38.943Z. Full domain 284/284 was at 23:21:34Z on `91f5fe4` and was not repeated.
- Touched emulator tests, not the full suites: interest/EMI restart 1/1 in 19.402s at 00:23:16Z. Feedback seed, `am force-stop`, read, both OK by 00:23:19Z. FinanceReview 3 and Run11 records 1, OK (4) in 14.712s ended 00:23:43Z. Live AMFI refresh OK (1) in 9.781s ended 00:33:52Z. gud_x86. TalkBack package is absent (`NO_TALKBACK`).
- Full data 99/99 and app 112/112 remain the 2026-11-02 runs on `91f5fe4`. They were not repeated after `7fba9c5`.
- `validate_schema.py` PASS and `security_preflight.py` PASS at 23:36:57Z. Not a pen test. Not re-run after Run 11.
- ARM64 debug sha256 `d5de1b9d…9476a70` size 50823136 at 00:34:37Z. Minified release sha256 `f0eb55e0…229aa2d` size 30755781 at 00:39:04Z. Both from `3fe140c`, native-code arm64-v8a. Phone not used. Test key, not a Play upload key.

## MATRIX
- P01 EMULATOR FirstRunIdentityInstrumentedTest, FirstRunIdentityShellTest, FirstRunIdentityHandoffInstrumentedTest — gap: phone install.
- P02 EMULATOR LocalAppPasswordInstrumentedTest, LocalLoginShellTest — gap: the OS credential Activity was not launched.
- P03 EMULATOR LegacyBackupShellTest, PortableRecoveryShellTest, LegacyVaultMigrationInstrumentedTest — gap: keep the restore warning.
- P04 EMULATOR NativeParitySchemaInstrumentedTest (schema v5) — gap: none for the migration.
- P05 LOCAL FinancialGoldenTest, M27CorrectionIntegrityTest; EMULATOR append-only via NativeParitySchemaInstrumentedTest — gap: not every event type has a UI restart.
- P06 EMULATOR FamilyAdministrationInstrumentedTest, FamilyOnboardingShellTest — gap: none for CRUD.
- P07 EMULATOR NativeCustodyHoldingInstrumentedTest, CustodyHoldingShellTest — gap: nominee UI is thin.
- P08 EMULATOR FamilyAdministrationInstrumentedTest (goals) — gap: goal UI is not in screenshots.
- P09 EMULATOR EightCategoryRestartInstrumentedTest — gap: search UI is only partial.
- P10 EMULATOR CustodyHoldingShellTest — gap: review step is not screenshotted.
- P11 LOCAL ManualAssetOnboardingTest, OriginalHoldingFormPolicyTest — gap: not every specialist field has an emulator restart.
- P12 LOCAL HoldingViewTest — gap: dated valuation UI is not separately tested.
- P13 EMULATOR NativeManualTradeInstrumentedTest and ManualEventRestartInstrumentedTest — gap: not screenshotted for every kind.
- P14 EMULATOR ManualEventRestartInstrumentedTest. Original row stays. A second correction of the same id is rejected. LOCAL ManualTradePlannerTest.
- P15 EMULATOR OwnerLensShellTest; LOCAL RecordedHoldingsSearchTest — gap: Vault/Settings/Licence have no owner filter.
- P16 EMULATOR Run8SurfaceInstrumentedTest sale sentences, inside app 112/112. Gain ₹196. Unknown opening is not ₹0. Screen does not write. Gap: no screenshot.
- P17 EMULATOR `recordsRouteOpensTheSavedStatement` (Run11, inside the 4-test run ended 00:23:43Z). Home opens Records and shows `Saved statement`. Registrar is not guessed.
- P18 EMULATOR NativeStatementApprovalInstrumentedTest, SyntheticIssuerImportInstrumentedTest — gap: real PDF BLOCKED-OWNER.
- P19 LOCAL CasIdentityMatchingTest. Printed name is not identity. Real PDF BLOCKED-OWNER. Not an emulator SQL import.
- P20 EMULATOR the same import restart. A second sha is ALREADY_IMPORTED. LOCAL VaultImportHistoryTest. Registrar is not guessed.
- P21 EMULATOR TaxOwnerScopeInstrumentedTest. FY 2024-25 note is exactly "Rates changed during this year; not estimated". Estimated tax is null, not ₹0.
- P22 EMULATOR `confirmedInterestAndEmiRestartOnceAndNotAsZero` 1/1 in 19.402s at 00:23:16Z, and `confirmTapPostsTheEngineInterestAndNotZero`. Confirm writes one INTEREST. A zero amount is refused. The list still says it does not write until that tap.
- P23 EMULATOR the same vault test writes one REPAYMENT of the instalment, not ₹0. A second id is not inserted. A paused loan is still omitted. The list does not write until confirm.
- P24 EMULATOR `savedBuyRestartsAsTheKnownXirrAndNotZero` and Run9 Insights sentences, both inside the full suites. Known text is `XIRR 10.01%. Estimate, not advice.` Unknown is `XIRR is unknown from this book.` The screen test injects the note. The vault test does not open Insights. Gap: no screenshot.
- P25 LOCAL HomeBooksTest; EMULATOR NativeShellTest and OwnerLensShellTest home — gap: books-health UI is not on Home.
- P26 LOCAL OfficialAmfiNavParserTest 4/4 at 00:29:38Z, including the Plan/Option columns. EMULATOR `explicitRefreshReadsThePublicAmfiFile` OK in 9.781s ended 00:33:52Z against https://portal.amfiindia.com/spages/NAVAll.txt. NSE/BSE live fetch was not run.
- P27 EMULATOR missing close and Run9 typed ₹125 stale, inside app 112/112. Not ₹0 and not the older ₹90. LOCAL ManualPriceTest. Gap: no screenshot.
- P28 EMULATOR NativeVaultInstrumentedTest, PortableRecoveryShellTest — gap: none new.
- P29 LOCAL BackupFolderViewModelTest — gap: no emulator test of the folder screen.
- P30 EMULATOR EmergencyExportShellTest; LOCAL EmergencyDossierCsvTest — gap: plaintext consent path.
- P31 EMULATOR `settingsSwitchesStartOffAndNameSgbAndDiagnostics` inside app 112/112. LOCAL `sgbToggleDoesNotInventTheMissingSide`. Gap: no screenshot.
- P32 EMULATOR DiagnosticNotesInstrumentedTest and the settings switch. PAN becomes `[redacted]`. A second repository read it. Process was not killed. Nothing is sent.
- P33 EMULATOR ScreenshotMatrixInstrumentedTest. Hindi stays वॉल्ट/स्टेटमेंट/क्लाउड. TalkBack not run.
- P34 LOCAL GmailProfileTest, GmailAccountListTest — gap: real OAuth BLOCKED-OWNER.
- P35 LOCAL MailAttachmentIntakePolicyTest, MailboxSyncTest; EMULATOR MailboxSyncSchedulerInstrumentedTest. Worker does not call Gmail. Real OAuth BLOCKED-OWNER.
- P36 EMULATOR feedback clock: seed, `am force-stop in.growurdhan.data.test`, then read. Both OK by 00:23:19Z. Does not open the store. Diagnostic notes were not force-stopped.
- P37 EMULATOR `restoreTapLeavesThePlanOff`. Empty Play evidence stays off. A button cannot turn the plan on. Real purchase BLOCKED-OWNER.
- P38 LOCAL PlayEntitlementPolicyTest, LicenceViewModelTest — gap: real Play purchase BLOCKED-OWNER.
- P39 Full domain 284, data 99, and app 112 were not re-run after `7fba9c5`. ARM64 debug and minified release were rebuilt from `3fe140c` and not installed. DEVICE parser, pen test, and owner sign-off stay BLOCKED-OWNER.
- E01 EMULATOR theme screenshots — gap: not every screen.
- E02 EMULATOR 48dp on Home, sale, records, price, feedback, first-run continue, and the SGB and diagnostics switches. TalkBack not run.
- E03 EMULATOR OwnerLensShellTest — gap: vault, settings, and licence.
- E04 LOCAL FamilyMemberOnboardingTest — gap: ownership-evidence UI.
- E05 EMULATOR first-run continue stays disabled until consent. The OS credential Activity was not launched.
- E06 LOCAL MailAttachmentIntakePolicyTest quarantine — gap: issuer confidence UI.
- E07 EMULATOR MailboxSyncSchedulerInstrumentedTest; LOCAL MailboxSyncTest — gap: real OAuth BLOCKED-OWNER.
- E08 EMULATOR unknown sale, missing close, and a stale typed price are not ₹0.
- E09 EMULATOR PortfolioInsightsShellTest and Run9 XIRR sentences. Gap: the XIRR screen test does not read the vault.
- E10 EMULATOR AMFI and NSE/BSE gateways, plus the typed stale manual price. Gap: no live fetch.
- E11 EMULATOR diagnostics start off, record one local note, delete it, and read it back. Nothing is uploaded. Process was not killed.
- E12 EMULATOR LegacyBackupShellTest, LegacyVaultMigrationInstrumentedTest — gap: none new.
- E13 EMULATOR ScreenshotMatrixInstrumentedTest onboarding — gap: data-quality coaching beyond books health.
- E14 LOCAL domain tests — gap: dependency graph is not written.
- E15 LOCAL docs/SBOM.md. `security_preflight.py` PASS at 23:36:57Z. Not a pen test.
- E16 EMULATOR restore tap stays unsubscribed. LOCAL LicenceGateTest. Real billing BLOCKED-OWNER.
- E17 NONE — CI is not used. Do not touch the trigger.
- E18 LOCAL docs/EGRESS.md. Finances, PAN, DOB, keys, and PDFs are not sent. Not a pen test.

## QUEUE
1. Let the Insights screen test read a vault, not an injected note.
2. Show Home books-health from HomeBooks. Do not invent a PAN warning.
3. TalkBack is not on this emulator image. Do not claim it ran.
4. Real NSDL PDF stays BLOCKED-OWNER. x86 cannot run pdfium.
5. Phone install stays BLOCKED-OWNER.
6. Production signing stays BLOCKED-OWNER.
7. Real Gmail consent stays BLOCKED-OWNER.
8. A real ₹99 Play purchase stays BLOCKED-OWNER.
9. NSE/BSE live fetch was not run. Do not invent a close.
10. Do not claim a Play-ready product.

## DECISIONS
- Hindi vault word is वॉल्ट. Statement is स्टेटमेंट. Cloud is क्लाउड. Banned: तिजोरी, बयान, बादल.
- FY 2024-25 equity tax is unknown. Note exactly `Rates changed during this year; not estimated`. Estimated tax is null, not ₹0. AY26-27 uses later rates only. Cess and surcharge are omitted. AY27-28 stays provisional.
- Sale golden: buy 10 at ₹100 with ₹10 charges, sell 4 at ₹150 with ₹4 charges, gain ₹196. Unknown cost stays `Need what you paid. The gain is not shown as zero.`
- A missing close stays `No close is saved. It is not zero.` A manual price wins and is stale after 7 days.
- Empty XIRR is exactly `XIRR is unknown from this book.` A known rate is an estimate, not advice.
- Saved imports are read from snapshot_import. The label is `Saved statement`. Do not invent NSDL or CDSL.
- Feedback clock survives `am force-stop` of `in.growurdhan.data.test`. Diagnostic notes were not force-stopped.
- Diagnostics default off. PAN and email become `[redacted]`. Token, password, pdf, and BEGIN lines are dropped. Notes are not uploaded.
- Interest and EMI post only after Confirm. A zero amount is refused. A second confirm of the same holding and date does not insert another row.
- AMFI's November 2026 file has Plan and Option columns. The older 6-column report still parses. Any other width fails closed. The request is the public NAV file only.
- Restore with no live purchase stays unsubscribed. A button cannot turn the plan on.
- Gradle: use `gud-gradle.sh`. Do not pass `--no-daemon` or a second `-p`. Do not run Gradle beside the emulator. Type tag 71 can print on a green run.
- Release uses the test keystore so R8 can run. That key is not a Play upload key.

## BLOCKERS
- BLOCKED-OWNER: real Gmail consent, Play purchases, production signing, phone install, real CAS PDFs, pen test, owner visual sign-off. DEVICE/OWNER none.
- Host RAM: do not assemble beside the emulator.
- `pypdfium2` is arm64-only. x86 CAS tests stay synthetic.

## KNOWN GAPS
- Records opens from Home and shows Saved statement. Sale and price screens are still synthetic rows.
- Confirm posts interest and EMI. ARM64 digests are from `3fe140c`. The release key is the test key. R8 mapping.txt was produced and not committed.
- TalkBack is not installed on `gud_x86`. The OS credential Activity was not launched. Insights was not screenshotted.
- Diagnostic notes were not proven across process death. A real CAS PDF was not parsed.
