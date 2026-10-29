# STATE

## HEADER
- Date: 2026-10-29 16:51–17:48 UTC. Run 5. Code head `c9623bc`. Docs commit follows.
- Emulator `gud_x86` API 35 at 1080×1920 / 480 dpi (360×640 dp). Gradle was stopped before `adb instrument`.
- Domain JVM `:domain:test` 270/270 (55 classes, 0 fail) at 17:46Z. The earlier 266 count was before `FyTaxEstimateTest`.
- Data 93/93 and app 94/94 finished 17:19:31Z, before the tax screen. Those counts do not include P21.
- ARM64 debug APK rebuilt 17:47:55Z from `c9623bc`, sha256 `ae0f08c2…cf8c`. Phone not used.
- Q10 release APK is the earlier tree `491ec67` (sha256 `22986ce1…fabc`). It does not include P21. `validate_schema.py` and `security_preflight.py` were not re-run.

## MATRIX
- P01 EMULATOR FirstRunIdentityInstrumentedTest, FirstRunIdentityShellTest, FirstRunIdentityHandoffInstrumentedTest — gap: phone install.
- P02 EMULATOR LocalAppPasswordInstrumentedTest, LocalLoginShellTest — gap: credential Activity UI test.
- P03 EMULATOR LegacyBackupShellTest, PortableRecoveryShellTest, LegacyVaultMigrationInstrumentedTest — gap: none new; keep the restore warning.
- P04 EMULATOR NativeParitySchemaInstrumentedTest (schema v5) — gap: none for the migration.
- P05 LOCAL FinancialGoldenTest, M27CorrectionIntegrityTest; EMULATOR append-only via NativeParitySchemaInstrumentedTest — gap: not every event type has a UI restart.
- P06 EMULATOR FamilyAdministrationInstrumentedTest, FamilyOnboardingShellTest — gap: none for CRUD.
- P07 EMULATOR NativeCustodyHoldingInstrumentedTest, CustodyHoldingShellTest — gap: nominee UI is thin.
- P08 EMULATOR FamilyAdministrationInstrumentedTest (goals) — gap: goal UI is not in screenshots.
- P09 EMULATOR EightCategoryRestartInstrumentedTest — gap: search UI is only partial.
- P10 EMULATOR CustodyHoldingShellTest — gap: review step is not screenshotted.
- P11 LOCAL ManualAssetOnboardingTest, OriginalHoldingFormPolicyTest — gap: not every specialist field has an emulator restart.
- P12 LOCAL HoldingViewTest — gap: dated valuation UI is not separately tested.
- P13 EMULATOR NativeManualTradeInstrumentedTest (BUY/SELL append) and ManualTradeShellTest — gap: no DIVIDEND/INTEREST/BONUS/REPAYMENT save-restart.
- P14 LOCAL ManualTradePlannerTest; EMULATOR ManualTradeShellTest loan OPENING_BALANCE picker and NativeManualTradeInstrumentedTest one VALUATION_UPDATE — gap: no SPLIT/CORRECTION/OPENING_BALANCE save-restart.
- P15 EMULATOR OwnerLensShellTest; LOCAL RecordedHoldingsSearchTest — gap: Vault/Settings/Licence have no owner filter.
- P16 LOCAL CostBasisTest, FinancialGoldenTest — gap: no sale P&L screen.
- P17 EMULATOR NativeShellTest `taxAndDocumentsOwnerScopeEntryPointsAreUserNavigable` — gap: no documents list screen.
- P18 EMULATOR NativeStatementApprovalInstrumentedTest, SyntheticIssuerImportInstrumentedTest — gap: real PDF BLOCKED-OWNER.
- P19 EMULATOR SyntheticIssuerImportInstrumentedTest (CDSL+NPS) and NativeCasIdentityBridgeInstrumentedTest; LOCAL CasIdentityMatchingTest — gap: NSDL hits `An enrolled CAS registrar is required.`; real PDF BLOCKED-OWNER.
- P20 EMULATOR NativeStatementApprovalInstrumentedTest — gap: history screen is thin.
- P21 LOCAL FyTaxEstimateTest 4/4 inside domain 270/270. Screen text is `FyTaxEstimate.Summary.lines()`, first line “estimate, not advice”. HouseholdDashboardViewModel wires it. The ViewModel unit test was not observed: the Gradle test worker died on a daemon handshake. Not on the emulator. Slab and property tax are not computed.
- P22 LOCAL InterestCatchUpTest, SavingsProjectionParityTest — gap: no interest screen.
- P23 LOCAL EmiPlanTest, FinanceParityExtensionsTest — gap: no EMI screen.
- P24 LOCAL ReturnMetricsTest, InsightsDrilldownTest; EMULATOR PortfolioInsightsShellTest — gap: InsightsScreen does not show XIRR.
- P25 LOCAL HomeBooksTest; EMULATOR NativeShellTest and OwnerLensShellTest home — gap: books-health UI is not fully asserted.
- P26 LOCAL OfficialAmfiNavParserTest, OfficialBhavcopyParserTest; EMULATOR OfficialAmfiCatalogGatewayInstrumentedTest, OfficialExchangeCloseGatewayInstrumentedTest — gap: no live fetch.
- P27 EMULATOR OfficialExchangeCloseGatewayInstrumentedTest cache — gap: no manual price override UI. A missing close must not become zero.
- P28 EMULATOR NativeVaultInstrumentedTest, PortableRecoveryShellTest — gap: none new.
- P29 LOCAL BackupFolderViewModelTest — gap: no emulator test of the folder screen.
- P30 EMULATOR EmergencyExportShellTest; LOCAL EmergencyDossierCsvTest — gap: plaintext consent path.
- P31 LOCAL AppearanceUseCasesTest — gap: SGB RBI-vs-market toggle.
- P32 NONE — gap: no opt-in diagnostics.
- P33 EMULATOR ScreenshotMatrixInstrumentedTest 7/7 (EN font 1 and 2, HI font 2, licence, terms, insights) — gap: TalkBack not run. Hindi body at 200% still scrolls; the cue is visible. Buttons आगे/छोड़ें.
- P34 LOCAL GmailProfileTest, GmailAccountListTest — gap: real OAuth BLOCKED-OWNER.
- P35 LOCAL MailAttachmentIntakePolicyTest, MailboxSyncTest; EMULATOR MailboxSyncSchedulerInstrumentedTest — gap: no mailbox-mapping UI; real OAuth BLOCKED-OWNER.
- P36 EMULATOR onboarding and terms in ScreenshotMatrixInstrumentedTest — gap: feedback/rating.
- P37 LOCAL LicenceTest, LicenceGateTest; EMULATOR licence shots — gap: restore purchases not exercised on the emulator.
- P38 LOCAL PlayEntitlementPolicyTest, LicenceViewModelTest — gap: real Play purchase BLOCKED-OWNER.
- P39 EMULATOR the suites above — gap: DEVICE parser, pen test, and owner sign-off stay BLOCKED-OWNER. Minified release compiled; it was not installed.
- E01 EMULATOR theme screenshots — gap: not every screen.
- E02 EMULATOR 48dp asserts and 200% shots — gap: TalkBack and contrast were not measured.
- E03 EMULATOR OwnerLensShellTest — gap: vault, settings, and licence.
- E04 LOCAL FamilyMemberOnboardingTest — gap: ownership-evidence UI.
- E05 EMULATOR LocalAppPasswordInstrumentedTest — gap: credential Activity UI test.
- E06 LOCAL MailAttachmentIntakePolicyTest quarantine — gap: issuer confidence UI.
- E07 EMULATOR MailboxSyncSchedulerInstrumentedTest; LOCAL MailboxSyncTest — gap: real OAuth BLOCKED-OWNER.
- E08 LOCAL InsightsDrilldownTest unknown cost — gap: stale price label on Home.
- E09 EMULATOR PortfolioInsightsShellTest drill-down — gap: goals series.
- E10 EMULATOR AMFI and NSE/BSE gateways — gap: manual override.
- E11 NONE — gap: consented diagnostics.
- E12 EMULATOR LegacyBackupShellTest, LegacyVaultMigrationInstrumentedTest — gap: none new.
- E13 EMULATOR ScreenshotMatrixInstrumentedTest onboarding — gap: data-quality coaching beyond books health.
- E14 LOCAL domain tests — gap: dependency graph is not written.
- E15 LOCAL not re-run this run — gap: SBOM not generated. Do not claim security_preflight.
- E16 LOCAL LicenceGateTest — gap: real billing BLOCKED-OWNER.
- E17 NONE — CI is not used. Do not touch the trigger.
- E18 LOCAL no cloud book — gap: egress inventory is not written.

## QUEUE
1. Emulator check of the tax screen. The ViewModel unit test did not finish.
2. P22 interest screen over InterestCatchUp.
3. P23 EMI screen over EmiPlan.
4. P13/P14 save-restart for DIVIDEND, INTEREST, BONUS, REPAYMENT, SPLIT, CORRECTION, OPENING_BALANCE.
5. NSDL staged import (CDSL and NPS synthetic path is in).
6. Manual price override next to the close cache. A missing close stays missing.
7. TalkBack on the screenshot screens.
8. Credential-screen Activity UI test.
9. Home and Settings in the screenshot matrix.
10. Rebuild the minified APK so it includes P21. The current release file is `491ec67`.

## DECISIONS
- Hindi vault word is वॉल्ट. Statement is स्टेटमेंट. Cloud is क्लाउड. Banned: तिजोरी, बयान, बादल.
- 200% Hindi keeps the type size. A scroll cue shows only when `canScrollForward` is true. Next/Back/Skip become आगे/पीछे/छोड़ें. Last page stays “Continue”.
- English price body now ends “You can cancel any time in Google Play.” Other English sentences were not strengthened.
- MailboxSync walks at most 5 pages, stores the next token, quarantines a wrong owner as UNLINKED_OWNER, and writes no ledger event. WorkMailboxSyncLauncher enqueues WorkManager. The worker calls MailboxSyncRuntime only. It does not call Gmail.
- Release `assembleRelease` compiled with the existing test keystore so R8 could run. That key is not a Play upload key. The APK is `release-artifacts/gud-arm64-release-testkey.apk`, not an unsigned build: the project refuses an unsigned release.
- Parser libraries stay inside `assets/chaquopy/*.imy` (pydantic_core 4839112, libpdfium.so 6393344), not under `lib/arm64-v8a`.

## BLOCKERS
- BLOCKED-OWNER: real Gmail consent, Play purchases, production signing, phone install, real CAS PDFs, pen test, owner visual sign-off. DEVICE/OWNER none.
- Host RAM: do not assemble beside the emulator. `-memory 2048` is raised to 2560 MB by the emulator.
- `pypdfium2` is arm64-only. x86 CAS tests stay synthetic.

## KNOWN GAPS
- Hindi body at 200% is still cut inside the card. The cue is the affordance. Do not claim “no clipping”.
- Q10 minify compiled. It was not run on the emulator or a phone. R8 keep rules were not loosened.
- NSDL enrolled import is still rejected. CAMS and KFINTECH are in the matcher but were not the synthetic emulator import.
- The minified APK on disk is still `491ec67` and does not include the tax screen.
- ARM64 parser behaviour is DEVICE: pending owner.
