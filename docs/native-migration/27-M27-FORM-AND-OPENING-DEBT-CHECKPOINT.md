# M27 holding form and opening debt source checkpoint
Date: 2026-10-16. Repository: vikaskunjbiharishah-ui/GUD, branch main. Starting HEAD bf4ecf3f358fad807deb808064c02e27dd2888e1. This is a SOURCE checkpoint, NOT compilation or milestone acceptance.

## Original source comparison
Reviewed original v9 types.ts (8 category types; 36 detail keys), AddHoldingModal.tsx (1,495 lines), HoldingDetailModal.tsx (847 lines), AddRecordModal.tsx (311 lines), prior native manual-trade ledger and rich holding profiles.

| Domain | New implemented work | Remaining literal parity and acceptance |
| --- | --- | --- |
| Product-specific create | OriginalHoldingFormPolicy enforces advanced initial NPS tier, FD compounding, gold purity, REIT/InvIT holding mode, AIF mode/category/commitment, MF custody mode and loan EMI/months/next-date. Compose, ViewModel and SQLCipher each enforce relevant advanced fields. Canonical 8-category/subtype basic registration preserved. | Original conditional 1495-line creation UI, market-security picker and some one-screen funded-start behaviors remain unproven; basic mode deliberately creates zero-unit/unknown-cost holdings. |
| Holding details | Owner-scoped clickable related loan/property selector, symmetric validated link, NSDL/CDSL pair validation, exact source enum values, stored nextEmiDate and existing rich metadata, family goals and source-preserving extras | Review every old field, including imported legacy review/day-change flags; actual UI owner acceptance pending. |
| User-started holdings | Saved-registration opens manually reviewed OPENING_BALANCE route rather than silently issuing a BUY. Users enter quantity, date and verified price; acquisition cost remains UNKNOWN until established. | Original one-submit initial quantity/balance creation is now two approvals. Product design deviation D-006 requires owner signoff. No actual Android E2E proof. |
| Loan amounts | One reviewer-entered unit priced as outstanding debt, with event/date/source and zero fees; duplicate and backdated input protections. Subsequent REPAYMENT logs cash, never invents principal. A separately reviewed dated zero valuation allows loan closure with settled liability balance, not with unrecorded principal assumptions. | Native overdue EMI scheduling and historical interest parity not proven; release cannot proceed without tests. |
| Native events | Original ten manual kinds retained, SNAPSHOT_ADJUSTMENT remains registrar-only, correction immutable and stale-book revalidated. Added liability opening correction. | Full ten-kind, replay, negative FIFO, owner-and-OS-challenge acceptance unrun on current SHA. |

## Verification, not claims
- Staged Kotlin sources: structural token balance 13/13, targeted allowlist/authorization/source checks.
- Local isolated Kotlin compiler: replicated product policy plus stub taxonomy compiled; six assertions passed. This is NOT a Gradle project compilation or current Kotlin source test.
- No Android SDK checkout was accessible for local project Gradle, JUnit or emulator. No GitHub Actions dispatched or paid build authorized. Do not call tests PASS on final SHA.
- Original legacy React/Tauri, CASPARSER, logo, signing and workflow YAML/trigger unchanged.
- Physical-device tests remain owner-deferred to M34.
- M25 and M26 remain OPEN; historical Build #18 was FAILED on older source SHA 3e41b7c4ac04261953beb75e79a57c6bdea3e6a3.

## Combined future regression plan
With owner approval, dispatch existing workflow native-android.yml with source_sha equal to current full 40-character main HEAD. Check native schema/security Python, JVM domain/app tests, Compose and SQLCipher API35 tests, debug assembly and disposable signed minified AAB. Specifically cover first-install password, recovery and old backup M25; family/HUF/nominee/goals M26; complete category wizard, AIF/FD/NPS/SGB/loans, ten events, corrections, details and encrypted backup M27. Observe terminal results, diagnose actual red jobs and retest the NEW fixed SHA. G14 light/dark/200% fonts/TalkBack and physical ARM64 signoff remain distinct acceptance gates.

## Gate
M27 issue #5 MUST remain OPEN. Original funded one-screen flow, listed-security picker, real Compose/SQLCipher execution and 200% UI accessibility still require parity/owner decisions; production readiness not claimed.
