# STATE

## HEADER
- Date: 2026-11-02. Run 10. Code `91f5fe4` (also `aa75081`). Docs after this line are not the APK commit. Previous code `ae68695`.
- Domain `:domain:test` 284/284 (61 classes, 0 fail). Latest XML 2026-11-02T23:21:34.790Z. Two cold workers refused before assertions. The reset retry is the result.
- Data 99/99 in 487.564s, ended 2026-11-02T23:32:54Z. App 112/112 in 130.016s, ended 2026-11-02T23:35:09Z. Emulator `gud_x86` 1080×1920 / 480. Gradle was stopped first. Shutdown exit 139 after `adb emu kill`.
- App JVM: only AppViewModelTest 1/1 at 23:16:24Z. The rest of `:app:testDebugUnitTest` was not re-run.
- `validate_schema.py` PASS. `security_preflight.py` PASS. Script unittest 35 OK at 23:36:57Z. Not a pen test.
- ARM64 debug sha256 `ccd47a51…94d77f2` size 50806772 at 23:37:19Z. Minified release sha256 `afa948e1…483c2de` size 30755781 at 23:42:27Z. Both from `91f5fe4`, native-code arm64-v8a. Phone not used. TalkBack not run. Test key, not a Play upload key.

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
- P17 EMULATOR `statementImportSurvivesRestartAndStaysWithItsOwner` inside data 99/99. Shell passes Ready lists into RecordsScreen. The Records route was not opened in a UI test. Label is `Saved statement`.
- P18 EMULATOR NativeStatementApprovalInstrumentedTest, SyntheticIssuerImportInstrumentedTest — gap: real PDF BLOCKED-OWNER.
- P19 LOCAL CasIdentityMatchingTest. Printed name is not identity. Real PDF BLOCKED-OWNER. Not an emulator SQL import.
- P20 EMULATOR the same import restart. A second sha is ALREADY_IMPORTED. LOCAL VaultImportHistoryTest. Registrar is not guessed.
- P21 EMULATOR TaxOwnerScopeInstrumentedTest. FY 2024-25 note is exactly "Rates changed during this year; not estimated". Estimated tax is null, not ₹0.
- P22 EMULATOR VaultFinanceReviewInstrumentedTest and FinanceReviewInstrumentedTest. Missing rate is not the published default. Screen does not post.
- P23 EMULATOR the same finance tests. A paused loan is omitted, not ₹0. Screen does not post.
- P24 EMULATOR `savedBuyRestartsAsTheKnownXirrAndNotZero` and Run9 Insights sentences, both inside the full suites. Known text is `XIRR 10.01%. Estimate, not advice.` Unknown is `XIRR is unknown from this book.` The screen test injects the note. The vault test does not open Insights. Gap: no screenshot.
- P25 LOCAL HomeBooksTest; EMULATOR NativeShellTest and OwnerLensShellTest home — gap: books-health UI is not on Home.
- P26 LOCAL OfficialAmfiNavParserTest, OfficialBhavcopyParserTest; EMULATOR AMFI and exchange gateways — gap: no live fetch.
- P27 EMULATOR missing close and Run9 typed ₹125 stale, inside app 112/112. Not ₹0 and not the older ₹90. LOCAL ManualPriceTest. Gap: no screenshot.
- P28 EMULATOR NativeVaultInstrumentedTest, PortableRecoveryShellTest — gap: none new.
- P29 LOCAL BackupFolderViewModelTest — gap: no emulator test of the folder screen.
- P30 EMULATOR EmergencyExportShellTest; LOCAL EmergencyDossierCsvTest — gap: plaintext consent path.
- P31 EMULATOR `settingsSwitchesStartOffAndNameSgbAndDiagnostics` inside app 112/112. LOCAL `sgbToggleDoesNotInventTheMissingSide`. Gap: no screenshot.
- P32 EMULATOR DiagnosticNotesInstrumentedTest and the settings switch. PAN becomes `[redacted]`. A second repository read it. Process was not killed. Nothing is sent.
- P33 EMULATOR ScreenshotMatrixInstrumentedTest. Hindi stays वॉल्ट/स्टेटमेंट/क्लाउड. TalkBack not run.
- P34 LOCAL GmailProfileTest, GmailAccountListTest — gap: real OAuth BLOCKED-OWNER.
- P35 LOCAL MailAttachmentIntakePolicyTest, MailboxSyncTest; EMULATOR MailboxSyncSchedulerInstrumentedTest. Worker does not call Gmail. Real OAuth BLOCKED-OWNER.
- P36 EMULATOR FeedbackClockInstrumentedTest and Run10 feedback tests inside the full suites. Does not open the store. Same-process DataStore. Process was not killed.
- P37 EMULATOR `restoreTapLeavesThePlanOff`. Empty Play evidence stays off. A button cannot turn the plan on. Real purchase BLOCKED-OWNER.
- P38 LOCAL PlayEntitlementPolicyTest, LicenceViewModelTest — gap: real Play purchase BLOCKED-OWNER.
- P39 EMULATOR domain 284/284, data 99/99, and app 112/112 on 2026-11-02. ARM64 debug and minified release were rebuilt from `91f5fe4` and not installed. DEVICE parser, pen test, and owner sign-off stay BLOCKED-OWNER.
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
1. Open Records from Home in a UI test. Keep the label Saved statement.
2. Let the Insights screen test read a vault, not an injected note.
3. Show Home books-health from HomeBooks. Do not invent a PAN warning.
4. TalkBack stays a gap. Do not claim it ran.
5. Process death of feedback and diagnostics is not proven.
6. Real NSDL PDF stays BLOCKED-OWNER.
7. Phone install stays BLOCKED-OWNER.
8. Production signing stays BLOCKED-OWNER.
9. Interest and EMI screens still do not post a ledger event.
10. Do not claim a Play-ready product.

## DECISIONS
- Hindi vault word is वॉल्ट. Statement is स्टेटमेंट. Cloud is क्लाउड. Banned: तिजोरी, बयान, बादल.
- FY 2024-25 equity tax is unknown. Note exactly `Rates changed during this year; not estimated`. Estimated tax is null, not ₹0. AY26-27 uses later rates only. Cess and surcharge are omitted. AY27-28 stays provisional.
- Sale golden: buy 10 at ₹100 with ₹10 charges, sell 4 at ₹150 with ₹4 charges, gain ₹196. Unknown cost stays `Need what you paid. The gain is not shown as zero.`
- A missing close stays `No close is saved. It is not zero.` A manual price wins and is stale after 7 days.
- Empty XIRR is exactly `XIRR is unknown from this book.` A known rate is an estimate, not advice.
- Saved imports are read from snapshot_import. The label is `Saved statement`. Do not invent NSDL or CDSL.
- Feedback clock and diagnostic notes are in the appearance DataStore. Proof is a second repository in the same process. Process death was not tested.
- Diagnostics default off. PAN and email become `[redacted]`. Token, password, pdf, and BEGIN lines are dropped. Notes are not uploaded.
- Restore with no live purchase stays unsubscribed. A button cannot turn the plan on.
- Gradle: use `gud-gradle.sh`. Do not pass `--no-daemon` or a second `-p`. Do not run Gradle beside the emulator. Type tag 71 can print on a green run.
- Release uses the test keystore so R8 can run. That key is not a Play upload key.

## BLOCKERS
- BLOCKED-OWNER: real Gmail consent, Play purchases, production signing, phone install, real CAS PDFs, pen test, owner visual sign-off. DEVICE/OWNER none.
- Host RAM: do not assemble beside the emulator.
- `pypdfium2` is arm64-only. x86 CAS tests stay synthetic.

## KNOWN GAPS
- Records, sale, and price screens are proven with synthetic rows. The Records route was not opened in a UI test.
- Interest and EMI screens do not post. ARM64 digests are from `91f5fe4`. The release key is the test key. R8 mapping.txt was produced and not committed.
- TalkBack was not run. The OS credential Activity was not launched. Insights was not screenshotted.
- Feedback and diagnostic notes were not proven across process death.
