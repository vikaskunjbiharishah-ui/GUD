> **Working repository:** https://github.com/vikaskunjbiharishah-ui/GUD.git — branch `main` only. `shahvikask/kosha` is the frozen original reference, not the repository to commit to.

# Original and native source inventory — initial pointer register

**Status:** Initial source-file inventory for the continuation of existing native development; **NOT exhaustive**. Audit screen/field parity against original exact reference during M27 and later milestones.

## Original product reference

- `shahvikask/kosha` `GUD-Android-Gmail@af96dcc8967ab595e1d9acee78e29e500cb99a9e` immutable source baseline.
- UI: `src/App.tsx`, `src/components/screens/{HomeScreen,HoldingsScreen,AccountsScreen,DocumentsScreen,FamilyScreen,AuthScreen,OnboardingScreen,LicenseScreen,TermsGate,FeedbackPrompt}.tsx`.
- Forms/details: `src/components/modals/{AddHoldingModal,HoldingDetailModal,AddRecordModal,EmergencySheetModal}.tsx`.
- Models: `src/types.ts` includes eight categories, 11 event kinds, 36-key `HoldingDetails` plus `Holding` metadata.
- Domain: `src/lib/{money,positions,lots,interest,emi,taxRules,returns,returnMetrics,growth,lookthrough,holdingForm,costBasis,snapshot,matchHolding,pan,homeDashboard}.ts`.
- Integrations: `src/lib/{gmailSync,statementMail,eodPrices,backup,backupIo,backupSchedule,auth,license}.ts`; schema `src/db/{schema,repository}.ts`; Tauri `src-tauri/`.
- **Frozen**: `src/casparser-main/**` and parser invocation/dependencies. Assets: `public/brand/{mark.svg,mark.png,logo.png,logo-full.png}`. Original mark navy `#07131F`, gold `#F5C14A`, teal `#2DD4BF`.

## Current native evidence

- Existing Android source under `android-native/`: Kotlin/Compose presentation, Kotlin domain and data repository, SQLCipher encrypted vault and Keystore, Chaquopy parser adapter boundary.
- Existing source checkpoints: [M27 implementation](../native-migration/27-M27-HOLDINGS-LEDGER-IMPLEMENTATION-CHECKPOINT.md), [M27 acceptance contract](../native-migration/27-M27-HOLDINGS-LEDGER-ACCEPTANCE-CONTRACT.md), [M24–M26 audit](../native-migration/24-26-PRE-M27-ACCEPTANCE-AUDIT.md).
- Previous M27 source mentions `ManualAssetOnboarding.kt`, `HoldingProfile.kt`, `HoldingRichMetadataContract.kt`, `HoldingProfileEditor.kt`, `ManualTradePlanner.kt`, `ManualTradeScreen.kt`, `SqlCipherVaultRepository.kt`, `CustodyHoldingScreen.kt` with new tests. **Presence and checkpoint statements are not proof of compilation, exact field parity or app-wide navigation.**

## Next inventory action

Inspect original `AddHoldingModal.tsx` all product-conditional fields and required validators line-by-line; compare each with native create/edit models, encrypted schema, backup format, Compose navigation and tests. Fill [parity-register.csv](parity-register.csv) per verified file and test; do not advance P09–P15 from source-level partial to accepted without the exact SHA run and required design/accessibility proof.
