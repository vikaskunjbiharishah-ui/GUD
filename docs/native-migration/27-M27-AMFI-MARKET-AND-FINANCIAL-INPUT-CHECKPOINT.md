# M27 — AMFI NAV source, safer financial metadata and incomplete official catalog

**Date:** 2026-10-16. **Repo:** vikaskunjbiharishah-ui/GUD **branch:** main. **Starting SHA:** 74178c356ff72524ba9791b457e1f29960abf298. **Resolve final SHA from GitHub after commit.**

**Status:** SOURCE INCREMENT ONLY. M27 issue #5 stays OPEN; no checked-in Android Gradle compilation or exact-SHA CI. M25 and M26 remain OPEN.

## Original vs native source audit

| Requirement | New source change | Remaining gaps and decisions |
| --- | --- | --- |
| P09/P11 type-sensitive holding fields | Existing eight-category original detail editor remains; metadata INR input for annual contributions, face value, AIF commitment/called capital and property income is now displayed in rupees and converted exactly to 1e4 scaled integer strings before SQLCipher edit. | Full source conditional UX, all holdings/v9 unknown field types and imported values still require device/golden coverage. |
| P10 existing initial amounts | Preserved D-006 two-stage creation: new zero-balance holding then separately reviewed device-approved OPENING_BALANCE with exact ID. No automatic BUY/cost lot. | One-submit original funded-form parity remains an explicit owner decision D-006 (PROPOSED, NOT APPROVED). |
| P10 market security selection | Added actual on-user-request AMFI fixed-origin HTTPS NAVAll.txt fetch, bounded 8 MB, redirects disabled, TLS-only, no wealth ID/query/upload, parser fail-closed on invalid old-format, date/ISIN/positive NAV checks, app-private no-backup atomic cache, offline search/quote freshness and user-visible Refresh button. | AMFI original-form text feed may be discontinued; confirmed current live format and runtime availability NOT VERIFIED. No NSE or BSE downloader or authenticated cache integrated. Both market exchanges remain M30 dependencies; market source parity is INCOMPLETE. |
| P12 edit/read | New INR display and exact persisted values for original moneyMinor fields; new test stages INR 12500.1234 as scaled 125001234. | Metadata is not a ledger acquisition. Checked-in Compose test not executed. |
| P13/P14 manual trade | Prior transaction category allowlists, immutability, D-006 and valuation/repayment behavior preserved. | Exact-current-SHA 10-kind JVM/emulator/SQLCipher and rollback run PENDING. |
| P15 usability | Official fund refresh is explicit, dated and offline-cache aware. No automatic app-start network request. | 200% fonts, TalkBack, visual G14 acceptance not proven. |

## Security and correctness

- Only hard-coded https://portal.amfiindia.com/spages/NAVAll.txt; no redirects; no holdings, PAN, PRAN, folio, family owner, security codes, credentials or user portfolio in the outbound request. Internet permission added; cleartext traffic remains prohibited and screenshot/backup protections unchanged.
- Parser accepts a bounded, strict semicolon six-field historic AMFI NAV report, verifies official format heading, ISIN, positive four-decimal NAV and nonfuture date; rejects HTML. A source URL/TLS cert authenticates the host, not correctness of an external issuer's underlying NAV.
- AMFI now advertises a newer report download format on its public website. Old text-feed availability requires actual Android endpoint verification. A 404/HTML/report change is a fail-closed error, not false current prices. Last valid cached rows stay available marked with their dates.
- App cache is public reports only, in app-specific no-backup storage, not the household encrypted wealth vault. Atomic write prevents partial accepted copies. User picks refresh; no background jobs.
- D-006 unapproved remains the only opening amount UX deviation. Never mark it as accepted based on this implementation.
- Unchanged source: original React/Tauri and parser sources/scripts, brand artwork, Gradle build/release signing and GitHub Actions YAML/CI trigger.

## Tests and evidence

- Five new source-authored tests: AMFI strict parser valid/invalid/stale (3 JVM methods), offline app-private cache instrumentation (1), AIF INR-to-scaled-string Compose (1). These tests have **NOT RUN**.
- Structural/contract source preflight on eight staged Kotlin files passed 8/8, plus exact source allowlist/provenance assertions. This is not compiler evidence. No Android SDK, Gradle, SQLCipher, Compose or emulator tests executed; no paid Actions dispatched. Actual Android tested source SHA: **NONE**.
- Previous [Build #18](https://github.com/vikaskunjbiharishah-ui/GUD/actions/runs/38237260337) FAILED on older 3e41b7c4ac04261953beb75e79a57c6bdea3e6a3. Do not upgrade historical conclusions.

## Acceptance matrix still OPEN

1. Verify actual current AMFI official report format and HTTPS endpoint in Android test; adapt to endorsed new download safely, without weakening parser/SSL checks. Current old endpoint may fail.
2. NSE and BSE official bhavcopy/security masters require source-specific archive formats, official URL and signatures/TLS/date validation, bounded ZIP/GZIP parser, owner-private cached search and dated quote provenance. Currently NOT IMPLEMENTED.
3. Full original AddHoldingModal.tsx/HoldingDetailModal.tsx/AddRecordModal.tsx conditional behaviors and 36-key plus rich field/legacy backup golden tests need execution. Reconcile user-entered opening balances vs D-006 with owner.
4. M25/M26/M27 combined exact-current-SHA regression needs explicit owner permission. Physical ARM64 and independent AppSec remain M34.

## Next exact regression

See docs/implementation/M25-M26-M27-EXACT-SHA-REGRESSION-CHECKLIST.md. When source gaps and D-006 choice are resolved, obtain owner approval, dispatch existing native-android.yml manually with source_sha=current exact main SHA, monitor terminal jobs, fix on main, retest corrected SHA (with permission). Do not trigger on commit.