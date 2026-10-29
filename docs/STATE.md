# STATE

## HEADER
- Date: 2026-10-29 22:25–23:16 UTC. Run 6. Code head `407bcf5`. Docs commit follows.
- Emulator `gud_x86` API 35 at 1080×1920 / 480 dpi. Gradle was stopped before `adb instrument`.
- Domain JVM `:domain:test` 271/271 (55 classes, 0 fail) at 23:09Z.
- Data 93/93 (475.981s) and app 97/97 (126.883s) finished 23:08:29Z. Those counts include the tax screen, the scrolled onboarding shots, and the interest/EMI review tests.
- `HouseholdDashboardViewModelTest` was not observed. The Gradle test worker died before any assertion (type tag 71).
- ARM64 debug sha256 `b2ce9de7…b2ab` and minified release sha256 `3b2cdd29…7a38`, both from `407bcf5`, native-code arm64-v8a. Phone not used. `validate_schema.py` and `security_preflight.py` were not re-run.

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
- P21 EMULATOR TaxOwnerScopeInstrumentedTest. FY 2024-25 shows the gains and the note "Rates changed during this year; not estimated". Estimated tax is null, not ₹0. One rate set is not applied across 23 July 2024. AY26-27 still uses 12.5% / 20% above ₹1.25 lakh. Cess is not included. AY27-28 stays provisional. The ViewModel unit test was not observed. Slab and property tax are not computed.
- P22 EMULATOR FinanceReviewInstrumentedTest (PPF note and amount from SavingsInterestEngine, same vector as InterestCatchUpTest). The screen does not write a ledger event. The Home route passes a null plan. Gap: not loaded from the open vault.
- P23 EMULATOR FinanceReviewInstrumentedTest (three due EMIs from EmiPlan, same vector as EmiPlanTest). A paused loan stays null in the engine. The Home route passes a null plan. Gap: not loaded from the open vault.
- P24 LOCAL ReturnMetricsTest, InsightsDrilldownTest; EMULATOR PortfolioInsightsShellTest — gap: InsightsScreen does not show XIRR.
- P25 LOCAL HomeBooksTest; EMULATOR NativeShellTest and OwnerLensShellTest home — gap: books-health UI is not fully asserted.
- P26 LOCAL OfficialAmfiNavParserTest, OfficialBhavcopyParserTest; EMULATOR OfficialAmfiCatalogGatewayInstrumentedTest, OfficialExchangeCloseGatewayInstrumentedTest — gap: no live fetch.
- P27 EMULATOR OfficialExchangeCloseGatewayInstrumentedTest cache — gap: no manual price override UI. A missing close must not become zero.
- P28 EMULATOR NativeVaultInstrumentedTest, PortableRecoveryShellTest — gap: none new.
- P29 LOCAL BackupFolderViewModelTest — gap: no emulator test of the folder screen.
- P30 EMULATOR EmergencyExportShellTest; LOCAL EmergencyDossierCsvTest — gap: plaintext consent path.
- P31 LOCAL AppearanceUseCasesTest — gap: SGB RBI-vs-market toggle.
- P32 NONE — gap: no opt-in diagnostics.
- P33 EMULATOR ScreenshotMatrixInstrumentedTest 7/7. At 200% the slide itself scrolls; there is no nested Surface. evidence/2026-10-29-2247 shows the last sentence fully: Hindi "रहता है।" and English "live on this phone." Buttons आगे/छोड़ें stay pinned. The top of a scrolled slide is off-screen. TalkBack not run.
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
1. Load interest and EMI from the open vault. Home currently opens an empty review.
2. P13/P14 save-restart for DIVIDEND, INTEREST, BONUS, REPAYMENT, SPLIT, CORRECTION, OPENING_BALANCE.
3. NSDL staged import (CDSL and NPS synthetic path is in).
4. Manual price override next to the close cache. A missing close stays missing.
5. TalkBack on the screenshot screens.
6. Credential-screen Activity UI test.
7. Home and Settings in the screenshot matrix.
8. ViewModel unit test. On this host the worker dies with type tag 71.
9. P16 sale P&L screen is still domain-only.
10. `security_preflight.py` was not re-run.

## DECISIONS
- Hindi vault word is वॉल्ट. Statement is स्टेटमेंट. Cloud is क्लाउड. Banned: तिजोरी, बयान, बादल.
- 200% Hindi keeps the type size. The slide column scrolls. Next/Back/Skip are आगे/पीछे/छोड़ें. Last page stays “Continue”. A scroll cue shows only while `canScrollForward` is true.
- FY 2024-25 (AY25-26) equity tax is unknown. s.111A and s.112A, Finance (No. 2) Act 2024, change on 23 July 2024. The ₹1,25,000 exemption is on the aggregate and the Act does not say which date-bucket is reduced first, so this app does not invent a split. Gains are still shown.
- AY26-27 uses only the later rates (LTCG 12.5% above ₹1,25,000, STCG 20%). Cess and surcharge are not included. AY27-28 stays provisional.
- Interest and EMI screens display the existing engines and do not post events. Home passes a null plan until a holding is loaded.
- Gradle recipe: `android-native/scripts/gud-gradle.sh`. It stops daemons, refuses to run if `adb devices` shows an emulator, then `gradle --offline --no-daemon --max-workers=2` with `-Xmx1280m` and client `GRADLE_OPTS=-Xmx384m`. `:app:testDebugUnitTest` still dies on type tag 71.
- MailboxSync walks at most 5 pages, stores the next token, quarantines a wrong owner as UNLINKED_OWNER, and writes no ledger event. WorkManager names `gud.mailbox.monthly` and `gud.mailbox.manual` are covered by MailboxSyncTest and MailboxSyncSchedulerInstrumentedTest. The worker does not call Gmail.
- Release `assembleRelease` uses the existing test keystore so R8 can run. That key is not a Play upload key.
- Parser libraries stay inside `assets/chaquopy/*.imy` (pydantic_core 4839112, libpdfium.so 6393344), not under `lib/arm64-v8a`.

## BLOCKERS
- BLOCKED-OWNER: real Gmail consent, Play purchases, production signing, phone install, real CAS PDFs, pen test, owner visual sign-off. DEVICE/OWNER none.
- Host RAM: do not assemble beside the emulator. `-memory 2048` is raised to 2560 MB by the emulator.
- `pypdfium2` is arm64-only. x86 CAS tests stay synthetic.

## KNOWN GAPS
- The 200% onboarding shot is scrolled to the end. The last sentence is fully visible. The first lines are off the top. Do not call the unscrolled position unclipped.
- Interest and EMI from Home do not read the vault. The instrumented tests pass a synthetic engine plan.
- Q10 minify compiled from `407bcf5` and was not installed. The keystore is the test key, not a Play upload key. R8 keep rules were not loosened.
- NSDL enrolled import is still rejected. CAMS and KFINTECH are in the matcher but were not the synthetic emulator import.
- ARM64 parser behaviour is DEVICE: pending owner.

