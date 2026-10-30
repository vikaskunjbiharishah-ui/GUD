# STATE

## HEADER
- Date: 2026-10-30. Run 8. Event restart remains `9b3227d`. This slice is sale, price, XIRR, NSDL match, records, and settings. SHA is the commit that contains this file once it is pushed.
- Domain slice XML at 13:45:39Z: SaleReviewTest 3/3, InsightsReturnsTest 1/1, Run8LibraryTest 3/3, ManualPriceTest 2/2, CasIdentityMatchingTest 12/12. Failures 0. Not a full `:domain:test`.
- LicenceViewModelTest 4/4 at 13:47:54Z. `Run8SurfaceInstrumentedTest` 8/8 in 22.663s, OK at 13:56:00Z, emulator `gud_x86` 1080×1920 / 480. First pass at 13:51:29Z was 6/8 and is not the result.
- Prior full suites stay the Run 7 numbers until this run repeats them: domain 273/273 at 05:08:23Z, data 94/94, app 97/97. ARM64 digests from `5f062e5` are stale once this code lands.
- `validate_schema.py` and `security_preflight.py` were not re-run this slice. Phone not used. TalkBack not run.

## MATRIX
- P01 EMULATOR FirstRunIdentityInstrumentedTest, FirstRunIdentityShellTest, FirstRunIdentityHandoffInstrumentedTest — gap: phone install.
- P02 EMULATOR LocalAppPasswordInstrumentedTest, LocalLoginShellTest — gap: the OS credential Activity was not launched.
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
- P13 EMULATOR NativeManualTradeInstrumentedTest (BUY/SELL) and ManualEventRestartInstrumentedTest (DIVIDEND, INTEREST, BONUS, REPAYMENT). 1/1 at 13:30:25Z. Gap: not screenshotted for every kind.
- P14 EMULATOR ManualEventRestartInstrumentedTest (SPLIT, CORRECTION, OPENING_BALANCE). Original row stays. A second correction of the same id is rejected. Wrong-owner dividend and cross-owner correction do not write. LOCAL ManualTradePlannerTest.
- P15 EMULATOR OwnerLensShellTest; LOCAL RecordedHoldingsSearchTest — gap: Vault/Settings/Licence have no owner filter.
- P16 EMULATOR Run8SurfaceInstrumentedTest `saleScreenShowsFifoGainAndFeeOrder` and `unknownSaleCostIsNotPrintedAsZeroEvenAtDoubleFont` (8/8 at 13:56:00Z). LOCAL SaleReviewTest. Gain ₹196; unknown opening is not ₹0. Screen does not write. Gap: no screenshot.
- P17 EMULATOR `recordsHideTheOtherOwner` hides the other owner. Shell route still passes empty lists. Gap: not read from SQL.
- P18 EMULATOR NativeStatementApprovalInstrumentedTest, SyntheticIssuerImportInstrumentedTest — gap: real PDF BLOCKED-OWNER.
- P19 LOCAL CasIdentityMatchingTest `enrolledNsdlIsinAndClientIdMapsWithoutUsingThePrintedName` (12/12 at 13:45:39Z). Printed name is not identity. Real PDF BLOCKED-OWNER. Not an emulator SQL import.
- P20 EMULATOR `recordsHideTheOtherOwner` shows that owner's NSDL batch. LOCAL `recordsAndImportHistoryStayWithTheOwnerAndIgnoreADuplicate`. Gap: shell does not read SQL.
- P21 EMULATOR TaxOwnerScopeInstrumentedTest. FY 2024-25 note is exactly "Rates changed during this year; not estimated". Estimated tax is null, not ₹0. AY26-27 uses 12.5% / 20% above ₹1.25 lakh. Cess omitted. AY27-28 provisional.
- P22 EMULATOR VaultFinanceReviewInstrumentedTest and FinanceReviewInstrumentedTest; LOCAL VaultFinanceReviewTest. Missing rate is not the published default. Screen does not post.
- P23 EMULATOR same finance tests. Paused loan is omitted, not ₹0. nextEmiDate is instrument metadata. Screen does not post.
- P24 LOCAL InsightsReturnsTest `knownFlowPrintsTheRateAndAnUnknownFlowIsNotZero`. InsightsScreen shows `xirrNote` on the household card. The emulator compared `InsightsReturns.note` on an empty book and did not open InsightsScreen.
- P25 LOCAL HomeBooksTest; EMULATOR NativeShellTest and OwnerLensShellTest home — gap: books-health UI is not fully asserted.
- P26 LOCAL OfficialAmfiNavParserTest, OfficialBhavcopyParserTest; EMULATOR OfficialAmfiCatalogGatewayInstrumentedTest, OfficialExchangeCloseGatewayInstrumentedTest — gap: no live fetch.
- P27 EMULATOR `missingCloseIsNotZeroAndManualPriceNamesItself` (missing label, zero `₹ 0.00` nodes). LOCAL ManualPriceTest. Gap: typed and stale labels were not rendered on the emulator.
- P28 EMULATOR NativeVaultInstrumentedTest, PortableRecoveryShellTest — gap: none new.
- P29 LOCAL BackupFolderViewModelTest — gap: no emulator test of the folder screen.
- P30 EMULATOR EmergencyExportShellTest; LOCAL EmergencyDossierCsvTest — gap: plaintext consent path.
- P31 LOCAL Run8LibraryTest `sgbToggleDoesNotInventTheMissingSide`. Settings default is RBI (`sgbUseMarket` false). A missing side stays missing. Gap: switch not in an emulator test.
- P32 LOCAL `diagnosticsStayOffAndDropSecrets`. Default off. Notes are not uploaded. Gap: not on the emulator; notes do not survive process death.
- P33 EMULATOR ScreenshotMatrixInstrumentedTest 7/7. Hindi vault/statement/cloud stay वॉल्ट/स्टेटमेंट/क्लाउड. TalkBack not run.
- P34 LOCAL GmailProfileTest, GmailAccountListTest — gap: real OAuth BLOCKED-OWNER.
- P35 LOCAL MailAttachmentIntakePolicyTest, MailboxSyncTest; EMULATOR MailboxSyncSchedulerInstrumentedTest. Worker does not call Gmail. Real OAuth BLOCKED-OWNER.
- P36 EMULATOR `feedbackPromptFollowsTheSnoozeAndDoesNotClaimTheStore` and `feedbackDismissHidesThePrompt`. Does not open the store. Gap: firstSeen is not stored across launches.
- P37 LOCAL LicenceViewModelTest `restoreWithNoLivePurchaseStaysUnsubscribed` (4/4 at 13:47:54Z). Screen says a button cannot turn the plan on. Gap: restore was not tapped on the emulator. Real purchase BLOCKED-OWNER.
- P38 LOCAL PlayEntitlementPolicyTest, LicenceViewModelTest — gap: real Play purchase BLOCKED-OWNER.
- P39 PARTIAL — full suites and ARM64 from this commit are not re-run yet. DEVICE parser, pen test, and owner sign-off stay BLOCKED-OWNER.
- E01 EMULATOR theme screenshots — gap: not every screen.
- E02 EMULATOR 48dp and contentDescription on Home, sale, records, price, feedback, and the first-run continue; 200% font on the unknown-cost sale. TalkBack not run. Settings, tax, interest, EMI, and manual trade were edited and not in this class.
- E03 EMULATOR OwnerLensShellTest — gap: vault, settings, and licence.
- E04 LOCAL FamilyMemberOnboardingTest — gap: ownership-evidence UI.
- E05 EMULATOR `firstRunIdentityStepStaysDisabledUntilConsentAndNamesTheDeviceLock`. Continue stays disabled and at least 48dp. The OS credential Activity was not launched.
- E06 LOCAL MailAttachmentIntakePolicyTest quarantine — gap: issuer confidence UI.
- E07 EMULATOR MailboxSyncSchedulerInstrumentedTest; LOCAL MailboxSyncTest — gap: real OAuth BLOCKED-OWNER.
- E08 EMULATOR unknown sale and missing close are not ₹0. LOCAL stale and manual labels. Gap: stale label not on the emulator.
- E09 EMULATOR PortfolioInsightsShellTest drill-down — gap: goals series. XIRR screen not instrumented.
- E10 EMULATOR AMFI and NSE/BSE gateways — gap: manual override UI not on the emulator beyond the missing close.
- E11 LOCAL `diagnosticsStayOffAndDropSecrets`. Nothing is uploaded. Gap: emulator switch.
- E12 EMULATOR LegacyBackupShellTest, LegacyVaultMigrationInstrumentedTest — gap: none new.
- E13 EMULATOR ScreenshotMatrixInstrumentedTest onboarding — gap: data-quality coaching beyond books health.
- E14 LOCAL domain tests — gap: dependency graph is not written.
- E15 LOCAL docs/SBOM.md from libs.versions.toml. `security_preflight.py` last PASS was 05:27Z. Not a pen test.
- E16 LOCAL LicenceGateTest and the restore test above — gap: real billing BLOCKED-OWNER.
- E17 NONE — CI is not used. Do not touch the trigger.
- E18 LOCAL docs/EGRESS.md. Finances, PAN, DOB, keys, and PDFs are not sent. Not a pen test.

## QUEUE
1. Open InsightsScreen on the emulator for a known XIRR and the unknown sentence.
2. Render a typed manual price and the stale label on the emulator.
3. Settings: diagnostics off, SGB RBI-vs-market, on the emulator.
4. Licence restore sentence on the emulator. Stay fail-closed.
5. Read import history and documents from the vault. The shell lists are still empty.
6. Persist the feedback clock. Do not open the store.
7. Full `:domain:test`, data and app suites, then both ARM64 builds from the final code commit.
8. TalkBack stays a gap. Do not claim it ran.
9. Real NSDL PDF stays BLOCKED-OWNER.
10. Phone install stays BLOCKED-OWNER.

## DECISIONS
- Hindi vault word is वॉल्ट. Statement is स्टेटमेंट. Cloud is क्लाउड. Banned: तिजोरी, बयान, बादल.
- FY 2024-25 equity tax is unknown. Note exactly `Rates changed during this year; not estimated`. Estimated tax is null, not ₹0. AY26-27 uses later rates only. Cess and surcharge are omitted. AY27-28 stays provisional.
- Sale fees follow LotEngine. Buy charges sit on the lot. FIFO removes the recorded unit price, not the charge-inclusive average. Sale charges reduce proceeds. Golden: buy 10 at ₹100 with ₹10 charges, sell 4 at ₹150 with ₹4 charges, gain ₹196. Unknown opening cost stays null: `Need what you paid. The gain is not shown as zero.`
- A missing close stays `No close is saved. It is not zero.` A manual price wins and is stale after 7 days. It is not zero.
- Empty XIRR is exactly `XIRR is unknown from this book.` A known rate is labelled an estimate, not advice.
- NSDL enrolled rows match EQUITY, GOLD_SGB, and MUTUAL_FUND. The printed name is not identity, same as CDSL.
- SGB `choose` returns null when the selected side is missing or not positive. It does not fall through.
- Diagnostics default off. PAN and email become `[redacted]`. Token, password, pdf, and BEGIN lines are dropped. Notes are not uploaded.
- Feedback does not open Play and does not collect a rating. Restore with no live purchase stays unsubscribed.
- Gradle: use `gud-gradle.sh`. Do not pass `--no-daemon`. Do not run Gradle beside the emulator. Type tag 71 can print on a green run.
- Release uses the test keystore so R8 can run. That key is not a Play upload key. Parser libraries stay inside `assets/chaquopy`.

## BLOCKERS
- BLOCKED-OWNER: real Gmail consent, Play purchases, production signing, phone install, real CAS PDFs, pen test, owner visual sign-off. DEVICE/OWNER none.
- Host RAM: do not assemble beside the emulator.
- `pypdfium2` is arm64-only. x86 CAS tests stay synthetic.

## KNOWN GAPS
- The 13:51:29Z surface run failed 2 of 8 because the same rupee text and the same missing-close sentence matched twice. The 13:56:00Z run is the one that passed. Do not cite the first pass as green.
- Records, sale, and price screens are proven with synthetic rows. Home's records route still passes empty lists.
- Interest and EMI screens do not post. ARM64 digests in SHA256.txt are still from `5f062e5` until the end rebuild.
- TalkBack was not run. The OS credential Activity was not launched.
