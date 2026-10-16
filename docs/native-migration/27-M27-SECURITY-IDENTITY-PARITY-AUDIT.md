# M27 Source Parity Audit: Security Picker, Opening Balances and Ledger
Date: 2026-10-16. Repo: vikaskunjbiharishah-ui/GUD main. Starting source SHA: 3ef250021d44170e8f2b7c7e23f153a76b5016a2. Final SHA: verify in GitHub. M27 issue #5 remains OPEN.

## Audited original references
Original src/types.ts, AddHoldingModal.tsx (1495 lines), HoldingDetailModal.tsx (847 lines), AddRecordModal.tsx (311 lines), v9 categories/details and source-price provenance; compared to current Compose, domain, SQLCipher, portable backup and manual ledger modules.

| Requirement | Implemented in this increment | Remaining acceptance or dependency |
| --- | --- | --- |
| P09 | All eight original categories remain, native alias categories can still record conservative transactions | Complete subtype-specific UI / screenshots and golden fixtures unverified |
| P10 | Created holding ID is handed directly to reviewed OPENING_BALANCE; transaction screen preselects that exact holding. Account-only Saved state cannot start an unrelated opening. | D-006 two-step UX vs original one-submit funded holding remains PROPOSED, not owner-approved |
| P10 / P11 | New typed LocalOfficialSecurityCatalog filters verified caller-supplied cached NSE/BSE/AMFI records: category/name/ISIN/symbol/code, latest nonfuture date, stale label and no automatically used stale price. Compose chooser can set name/ISIN/symbol and provenance/dated price only from fresh rows. | There is NO real authenticated official catalog loader wired into native application yet. Current deployed catalog is empty. Genuine NSE/BSE/AMFI downloads belong to M30. Do not claim working public security search or current quotes |
| P10 / P12 | Duplicate same-owner ISIN rejected in Compose, VM and SQLCipher; existing CAS identities checked. On rejection atomic creation rolls back. Explicit purchase should target the already tracked position. | End-to-end duplicate redirect UI and cross-account security matching require device tests / owner decision |
| P11 / P12 | Existing 36 detail and instrument attributes retained, new instrument fields included in portable encrypted backup roundtrip fixture. | Full historical unknown-fields/legacy migration and mandatory conditional form behaviors still pending |
| P13 / P14 | Shared category allowedKindsFor for 10 manual kinds, also recognizes old native category aliases; disallowed types rejected in domain and not offered by UI. Switching holding/type clears typed units, price and charges. Owner credential + immutable write gates unchanged. | Full ten-kind exact-SHA JUnit/Compose/SQLCipher and golden FIFO acceptance unrun |
| P15 | Owner-scoped saved holding selection, account only cannot route into arbitrary asset opening, source/stale quote labels and duplicate warnings | Full visual G14, TalkBack 200% font, landscape/tablet and UI reviews pending |

## Source code and verification
The source commit contains changes in ManualTradePlanner, ManualTradeScreen, CustodyHoldingViewModel, CustodyHoldingScreen, GrowUrDhanShell, HoldingProfileEditor, one SQLCipher repository safeguard, domain market source, plus 12 new synthetic JVM/Compose/SQLCipher test methods. Existing scripts, native worklows and frozen React/Tauri/CASPARSER/brand remain untouched.
- 12 Kotlin source/test staged files passed balanced lexer checks and targeted static source-invariant assertions (not a compiler).
- Exact GitHub blob bytes of new LocalOfficialSecurityCatalog and existing OfficialQuotePolicy were reproduced, Git hashes matched, isolated kotlinc compilation with MoneyScaled model stub succeeded. Seven standalone offline catalog assertions passed. NOT an Android Gradle, instrumented or JUnit run.
- New Kotlin/JUnit/Compose/SQLCipher tests were authored but NOT run. No Android SDK private checkout, full test suite, APK, AAB or device results.
- GitHub Actions was NOT dispatched and source SHA has NO passing exact-SHA CI result.
- Last combined Build #18 FAILED on older source SHA 3e41b7c4ac04261953beb75e79a57c6bdea3e6a3.
- M25 issue #3, M26 issue #4 and M27 issue #5 remain OPEN. Real ARM64/independent AppSec owner acceptance belongs to M34.

## Owners and decisions
D-006 two-stage OPENING_BALANCE instead of original one-submit holding+BUY is an UNAPPROVED UX deviation. Do not silently mark parity complete. No new external-provider integration was claimed. M30 official data fetch and cache, G14 visual/a11y, M27 original all-conditional field parity, device acceptance and one combined owner-approved exact-main-SHA regression remain open.
