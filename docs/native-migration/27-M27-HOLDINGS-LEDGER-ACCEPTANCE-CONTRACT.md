# M27 — audited implementation and acceptance contract

Date: 2026-10-12. Repository: `vikaskunjbiharishah-ui/GUD`, branch `main`.
Authority: [M27 issue #5](https://github.com/vikaskunjbiharishah-ui/GUD/issues/5), original `src/types.ts`, `src/components/modals/AddHoldingModal.tsx`, `HoldingDetailModal.tsx`, `AddRecordModal.tsx`, and native module code. This document is a scope inventory **only**, not acceptance or an implementation-complete claim.

## Completion gate (all required; no silent deferral)
1. Eight canonical categories, complete subcategories and original asset-type form behavior: EQUITY, MUTUAL_FUND, GOLD_SGB, FIXED_INCOME, REAL_ESTATE, SPECIALISED, CASH_BANK, LIABILITY. Explicitly handle NPS, PPF/SSY, EPF, FD, SGB, REIT/InvIT, AIF, property, physical gold, bonds and loans. Existing aliases like NPS/PPF/EPF/GOLD/OTHER cannot be mistaken for the eight persisted v9 categories without an explicit lossless mapping.
2. Holding create/edit/detail/archive/close, correct ownership, account linkage and safe state transitions. Preserve acquisition cost as unknown until verified; do not fabricate transactions.
3. Every v9 `HoldingDetails` field exposed by `ReactV9HoldingDetailsContract.fields` must have a validated write path and readable/editable native presentation, or a specifically reviewed lossless exception. Preserve other original holding fields: dates, maturity/compounding/rates, deposits, NPS tier/CRA/PFM, MF plan/option/demat, owner %, goals, linked loan, notes/location, price/provenance/as-of and identifiers.
4. Owner-authenticated, review-before-commit ledger entry for BUY, SELL, DIVIDEND, INTEREST, BONUS, REPAYMENT, SPLIT, CORRECTION, OPENING_BALANCE and VALUATION_UPDATE; keep SNAPSHOT_ADJUSTMENT exclusively in registrar-controlled import reconciliation. Prove that irrelevant kinds cannot be used for a selected asset.
5. Corrections are **new immutable events** referencing superseded originals. Preserve fee and lot identity, reject cycles/foreign-owner references/invalid amounts/dates, rerun chronological FIFO and reject negative holdings. Atomic write with stale-book revalidation under SQLCipher transaction.
6. Complete user-visible search, family/category/account/closed filters, detailed event timeline, exact history and linked-account drilldowns; accessible empty/error/large-font states.
7. Automated acceptance: category/subcategory golden fixtures, every details field round-trip including historic JSON, create/edit/archive/restore, price/source/date semantics, event kind-by-kind plan/commit, correction supersession, duplicate/replay, date/scales/overflow, FIFO and backdated negative-position rejection, owner isolation, rollback, UI navigation and encrypted portable-backup round-trip. Exact source SHA Android/JVM/security/release regression needed before feature milestone closure. Physical handset testing is deferred to M34 by owner but remains a release gate.

## Observed starting state (not M27 completion)
- `android-native/domain/src/main/kotlin/in/growurdhan/domain/vault/ManualAssetOnboarding.kt` permits eleven broad category values, including NPS/PPF/EPF, instead of the canonical eight-category/subcategory wizard. Creates zero-unit/zero-price holdings.
- `android-native/app/src/main/java/in/growurdhan/app/presentation/screens/CustodyHoldingScreen.kt` offers mutual-fund CAS enrolment and name/category-only manual registration; no full holding details create/edit workflow.
- `android-native/domain/src/main/kotlin/in/growurdhan/domain/finance/ManualTradePlanner.kt` and manual-trade UI currently implement user-initiated BUY and SELL only.
- `android-native/domain/src/main/kotlin/in/growurdhan/domain/finance/FinancialEvents.kt` already enumerates all 11 event kinds, but an enum is not a user-visible authorized and persistent workflow.
- `android-native/core/model/src/main/kotlin/in/growurdhan/core/model/ReactV9ParityContract.kt` inventories the original detail keys; schema JSON preservation is not the same as typed field editing and round-trip parity.

## Safety and workflow
- Do not edit the frozen CASPARSER, original functional reference branch or native regression CI marker while developing M27.
- Implement on `main` only as instructed. Routine commits must not be represented as a passed build.
- Do not close #5 or trigger a GitHub Actions build/regression until all source requirements and local tests are implemented and the owner is told explicitly whether the milestone is complete.
