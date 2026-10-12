# M25 and M26 — implemented source contract and accepted safety boundaries

**Date:** 12 October 2026. **Repository:** `vikaskunjbiharishah-ui/GUD`, `main`. **Acceptance status:** source checkpoint; not a claim of passing any new Android run or physical-device testing.

## M25 — Original-owner onboarding and login

A newly enrolled native local app password can be paired with the owner email during device-approved enrollment. A nonexportable Android Keystore HMAC binds the normalized email to the verifier; **plaintext email is never stored in login SharedPreferences**. For already-enrolled native users, the hardware-MAC email binding is provisioned after their first successful, separately device-approved SQLCipher primary-account registration, provided the email matches any existing binding. Existing verifiers without a binding keep backwards-compatible password login; they are not reset or silently replaced. A binding mismatch revokes the current password admission, and the financial vault still needs independent OS credential confirmation.

The original first/last name, PAN, DOB, and two distinct security-question IDs/independently salted 210k PBKDF2 answer verifiers are kept inside the v4 SQLCipher owner identity table. Public projections show only masked PAN. The native login workflow deliberately uses an email-and-password step followed by Android unlock for enrolled email-bound accounts; original React/Tauri password-vault internals and Android native SQLCipher wrapping keys are **different and never treated as interchangeable**.

**Deliberate security decision:** the original two human security answers cannot *alone* replace a Keystore-bound native verifier. Native forgotten-password recovery requires the previously enrolled 256-bit offline single-use code **AND** a fresh Android screen lock. Both original answers remain mandatory for authenticating an old GUDVLT01/KOSHA1 backup. This retains the user's historical recovery evidence without offering a weak reset of a native cryptographic access control. The loss-of-both-secrets case requires a separately encrypted native/old backup on a new installation, not a destructive phone reset. Mandatory 180-day password renewal, failure cooldown and background lock remain present.

## M25 — Old encrypted backups and dry-run conflict policy

The original binary `GUDVLT01` and historical JSON `KOSHA1` encrypted archives are processed only after original account identity and Android device challenge. The migration protocol is now:

1. Read-only preview with coarse family/account/holding/event/goal counts and source SHA-256. A preview **never** claims that every row has already been accepted.
2. Confirm original email/password, PAN, DOB and both question answers with a **new** Android device challenge. Run the actual legacy-to-v4 conversion **entirely in memory**, validate the source row structure, and show the per-table mapped-count report, original-JSON-preservation count and source fingerprint. No existing native row is mutated during this dry-run. Unrecognized unsupported collections fail closed.
3. Require an explicit **second Android device credential** before any write. Reopen the selected SAF archive and **reject if its SHA-256 differs from the inspected source**. Perform the canonical re-authentication and transactionally restore into **only an empty native SQLCipher book**. On failure, rollback and preserve both original archive and any preexisting data.

**Conflict decision:** "Create a new empty vault" is supported; overwrite and merge into an occupied native book are deliberately forbidden. There is no unsafe automatic reassignment or fabricated financial purchase event. A full resumable source migration checkpoint is not required when an entire bounded file is authenticated, converted and imported in one atomic transaction; the *unchanged original encrypted archive* is the retry checkpoint. The encrypted destination also retains source SHA and original format/version. No original PAN, login secret or PDF bytes are written to logs or plaintext files. Some original complex fields remain accessible through lossless encrypted `extra_json` and future native holdings/tax/document milestones will expose them as typed UI.

## M26 — Family selection and joint-account policy

Family editing, reversible deactivation, owner-scoped custody and nominees, goals, many-to-many goal tags, HUF entity classification, and financial-owner validation are in SQLCipher v4. The native screen provides grouped issuer/custodian accounts, search, statuses, linked holdings and only masked identifiers.

One **ephemeral owner selector** is now threaded through Home, Portfolio, Accounts, family/goals, custody/holdings, CAS Statement Review, Documents (the consent-gated CAS workflow), and a read-only Tax owner scope entry. Tax scope shows *recorded* owner portfolios only; it does not invent taxable gains or tax dues. Rich document archives and tax calculation belong to later milestone modules, which must reuse this same selector before their release acceptance.

**Joint account decision (strict v4 source parity):** the original `DematOrFolioAccount` and the current encrypted custody-account/ledger FKs contain exactly one legally *accountable* `owner_id`. HUF and Joint **family relations** are supported as owner entities, but this must not be misrepresented as independently fractional co-ownership of one account by two members. Splitting shared beneficial shares requires a separately approved v5 data schema and tax/lot/corporate-action allocation policy. We do not duplicate holdings across owners or invent shares; cross-owner reassignment remains blocked. Synthetic two-member/HUF tests check no ledger cascade or owner substitution.

## Automated acceptance and deferred physical gate

Earlier [Build #15](https://github.com/vikaskunjbiharishah-ui/GUD/actions/runs/38134761299) passed domain/JVM, debug APK and release AAB but was red on two Android instrumentation tests (112/114 pass). Its test fixes are in commit `2bed985`, and additional source patches require a new **single exact-source-SHA combined M25+M26 regression** for closure. Automated acceptance must pass tests for native password/HMAC/OS recovery, old archive identity/dry-run/fingerprint and empty-book rollback, native HUF/Joint constraints, owner selector, and v4 encrypted backup restoration.

Physical handset and new-device restore tests are **explicitly deferred by the owner to M34** after all feature milestone closures. They remain a mandatory release gate before commercialization; emulator success is not real hardware approval.

**Do not close #3 or #4 based on this document alone.** Close the applicable development scopes only after their exact-SHA combined regression passes and explicitly recorded security/product decisions above are accepted as scoped parity. M27 source work should not be conflated with M25/M26 acceptance.
