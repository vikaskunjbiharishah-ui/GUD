> **Working repository:** https://github.com/vikaskunjbiharishah-ui/GUD.git — branch `main` only. `shahvikask/kosha` is the frozen original reference, not the repository to commit to.

# GrowUrDhan E2–E4 Commercial Readiness Milestone 22

**Date:** 10 October 2026  
**Repository:** `vikaskunjbiharishah-ui/GUD`  
**Branch:** `main` only  
**Status:** Implementation staged for one source commit; verification pending.  
**No writes to:** `shahvikask/kosha`, `GUD-Android`, `GUD-Android-APK`,
`GUD-Android-ChatGPT`, or frozen `src/casparser-main/**`.

## User-visible code delivered

Manual transactions are entered at Portfolio → Record an explicit buy or sell.
The form selects an already enrolled holding, trade date, quantity, unit
price and charges. The user first sees gross, fees, cash flow, owner and
quantity, and only the Activity can request a fresh Android device credential
and call SaveAfterCredential. The ViewModel re-reads the authorized book,
rejects a different credential challenge, and passes the review to a
**transactional repository method**. The SQLCipher implementation re-derives
the draft from a canonical SHA-256 book snapshot inside its own write
transaction; changed members, accounts, holdings or events invalidate the
review. The existing append-only DB trigger and pre-commit approval check
remain intact. One event is inserted; no statement import auto-posts trades.

Safeguards: exact INR[4]/quantity[8] numeric parsing; BigInteger multiplied
amounts; no liabilities, future dates or cross-owner holdings; SELL requires
proven historical acquisition lots and cannot be backdated to oversell.
Unknown cost is never invented. Saved/failed state does not claim success
after lock/re-authentication. The most recent 20 event summaries remain
visible only while the vault is unlocked, without internal event or folio IDs.

## Foundation modules (explicitly NOT live service integrations)

| Area | Code | What it does not do |
| --- | --- | --- |
| Gmail | `domain/mail/MailAttachmentIntakePolicy.kt` | Does not request OAuth, fetch mail, parse PDFs, persist tokens or run WorkManager |
| Market | `domain/market/OfficialQuotePolicy.kt` | Does not fetch AMFI/NSE/BSE quotes, cache prices or value live holdings |
| Play | `domain/commercial/PlayEntitlementPolicy.kt` | Does not connect BillingClient, verify signatures, acknowledge real purchases or grant a native licence |
| Release | `scripts/release_gate.py` and protected signed AAB workflow | Does not produce independent security proof or substitute for owner/Play approvals |

Gmail `gmail.readonly` is a **restricted** scope subject to Google OAuth
verification. Public access must follow Google's permitted-use policy.
Google Play billing requires correct purchase-state handling and
acknowledgement. A production verifier and product setup must be designed
and approved without leaking local wealth records to GrowUrDhan servers.
AMFI NAV download formats and history windows are subject to the current
official website terms and available formats.

## Regression cases added

- Domain: exact buys/sells, fractional rounding, overselling, old lots,
  unauthorized owners, malformed dates/amounts, book digest changes,
  linked shared inboxes, quarantine/size/deduplication, stale official quotes,
  unverified/pending/expired Play entitlements.
- ViewModel: forbidden locked writes, fresh credential after staging, stale
  read/review rejection, no double-submission, changed-book refusal.
- Compose: explicit staged-only form, review cash amounts and locked UI.
- Encrypted Android: isolated SQLCipher buy/sell persistence, duplicate
  event rejection, CAS-only unsaleable units, stale concurrent review rollback.
- Python security/release gates: repository auth wrappers, atomic trade
  insertion checks, credential route, all mandatory owner release documents,
  exact source SHA binding and tampered proof detection.

**Execution:** These suites remain unexecuted until the next GitHub
`GrowUrDhan Native Android M3` manual workflow. Current-commit compilation,
instrumentation, real ARM64 parser, app-specific login, accessibility at
200% fonts and independent MASVS penetration testing are not yet verified.

## External blockers that a commit cannot close

1. **App login and migration:** native app-password model and full legacy
   books/backup migration remain unimplemented. Device screen lock is not
   the same as original GrowUrDhan login.
2. **NPS/CAS breadth:** verified PRAN/NSDL/CDSL identity and real ARM64
   parser/device acceptance are missing; frozen CASPARSER must stay intact.
3. **Gmail:** Google OAuth app/client configuration, allowed-use review,
   restricted-scope verification, WorkManager, encrypted mailbox state,
   multi-account consent and revocation still needed.
4. **Official market prices:** compliant AMFI/NSE/BSE adapters, source dates,
   caching, errors and no-PAN/network evidence still needed.
5. **Play commercial:** real product IDs/config, purchase verification,
   acknowledgement/restore, trial policy and lawful merchant information
   still needed. The pure domain gate is not a Billing client.
6. **Legal/AppSec/release:** owner-approved terms/privacy, actual production
   signer, independent OWASP MASVS assessment, physical API device/ARM64
   validation and cross-device backup recovery remain open.

No developer or CI fixture is a signed production-release approval.
`native-signed-aab.yml` refuses a release build until a matching
`docs/release-evidence/release-approval.json` and acceptance files exist.
Current development does **not** generate that manifest.

The next release milestone must not be described as complete merely
because this integrated source commit is present.
