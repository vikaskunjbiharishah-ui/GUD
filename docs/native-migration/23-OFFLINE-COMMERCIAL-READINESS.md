> **Working repository:** https://github.com/vikaskunjbiharishah-ui/GUD.git — branch `main` only. `shahvikask/kosha` is the frozen original reference, not the repository to commit to.

# GrowUrDhan — Offline Commercial Roadmap Increment (Milestone 23)

**Document date:** 10 October 2026  
**Source branch:** `vikaskunjbiharishah-ui/GUD/main` only  
**Scope:** features not requiring Google Cloud or Google Play Billing  
**Verification:** source staged; current-commit Gradle and device tests have NOT run.

## Delivered application workflows

### 1. Local app-password admission and rotation

The original `src/lib/auth.ts` specified a locally verified password,
8+ characters with upper/lowercase, digit and special character, PBKDF2-SHA256
210,000 iterations and a 180-day rotation policy. New
`domain/vault/LocalPasswordRules.kt`,
`app/presentation/LocalAppPasswordStore.kt`,
`screens/LocalLoginScreen.kt` and
`screens/ChangeLocalPasswordScreen.kt` implement a first-install password
verifier, protected sign-in, persistent wrong-password cooldown and
re-verification on password change. The verifier uses randomized PBKDF2
input and a non-exportable Android Keystore HMAC key; only salt/tag/
iteration metadata are retained in private preferences.

**Two separate authorizations are required:** an app-password verification
and a successful Android device-lock challenge before SQLCipher opens or
financial writes are approved. Production repository authorization routes
through `VaultSessionGate`, and the gate checks both factors. UI credentials
are not written into saved Compose state. A device credential alone must
not be enough. Opening the native app does not automatically create a vault.

**Important cryptographic distinction:** the new password does NOT decrypt
the native database or replace the existing Keystore-wrapped SQLCipher key.
Password rotation does NOT re-encrypt the existing book. Lost password,
invalid Keystore key or missing verifier metadata is a recovery blocker;
there is no insecure reset, data-clearing or silent verifier replacement.

**Not equivalent to full legacy login parity:** the native app still lacks
original account-email/profile enrollment, security-question and PAN/DOB
recovery compatibility, automatic legacy password migration and a verified
complete password rotation UX at 180 days. Legacy PBKDF2 verification is
not portable automatically without the original account record and
owner-controlled migration. Production admission needs real-device testing.

### 2. Offline asset and account enrollment

`ManualAssetOnboarding`, `CustodyHoldingViewModel`, the native Custody
screen and SQLCipher repository register owner-scoped **zero-unit and
zero-value** assets for NPS, PPF, EPF, equities, gold, bank/cash, funds,
bonds and property. Optional NSDL/CDSL/NPS/BANK/BROKER accounts are
**nickname-only** and cannot become verified registrar identities merely
because the user chose their type. Existing CAMS/KFINTECH CAS identity
enrollment stays strict and frozen parser sources are unchanged.

New asset registration requires a new OS credential, remains an append-only
financial-book mutation and creates **no synthetic BUY, opening position,
statement transaction, PRAN or historic cost**. Users see manually tracked
assets separately from enrolled CAS mappings.

**Not yet complete:** real NPS PRAN identity and statement imports,
government account feeds, demat/NSDL/CDSL parser validations, loans/EMI
editing, authoritative prices, and full account detail parity.

### 3. Original encrypted backup inspection — read-only

`LegacyLockedBackupInspector` recognizes the original
`GUDVLT01` binary `.kosha` format (600,000-round PBKDF2-SHA256,
`GrowUrDhan\\n<normalized-email>\\n<password>`, AES-256-GCM).
The user selects a local/cloud SAF file and provides the backup email
and password after separately confirming Android device credentials.
The code authenticates ciphertext under bounded input and reports **only
counts** for members, holdings, events, accounts and goals, plus presence
of extended metadata and CAS history. It neither writes nor merges into
the native SQLCipher database. Inputs and decrypted plaintext buffers
are cleared after inspection.

**Lossless migration remains blocked:** the original backup contains
goals, extended holding metadata, acquisition details and statement
histories not fully represented in native schema v3. Blind import would
be destructive. A complete mapper and schema/backup upgrade with golden
tests and new-device security-answer/PAN checks are required separately.
This screen is not a general backup restore or password-reset facility.

### 4. User-directed emergency CSV

Settings offers an **explicitly unencrypted** emergency summary.
The user chooses a Storage Access Framework document destination, sees a
plaintext/privacy warning and completes a new Android credential challenge.
It derives family position from one authorized book snapshot, refuses
unreconciled totals, exports source/as-of and unknown-cost warnings,
and protects common spreadsheet readers against formula injection.
The CSV omits passwords, PAN, folio, raw IDs and fabricated returns.
No auto-upload is performed by GrowUrDhan; a user-selected SAF cloud
provider **may itself upload the CSV**, which is why the UI warns.
A full recovery still requires the encrypted native backup.

## New regression coverage

- Unit: original app-password policy, rotation dates, offline asset
  categories/uniqueness, emergency CSV exactness and CSV injection.
- Android instrumentation: Keystore/password create/verify/restart/rotate,
  fail-closed corrupt verifier, authenticated synthetic original `.kosha`
  archive inspection/tamper rejection, and real SQLCipher zero-unit NPS
  holding persistence.
- Compose: login setup/locked privacy, read-only legacy backup,
  and unencrypted CSV warning.
- Security preflight: fail if repository loses approved
  `registerManualAsset` route, if app/OS dual authorization disappears,
  or if the CSV/legacy-inspection trust boundaries are removed.

These test files are **written, not executed** on this SHA.
The next dedicated GitHub native build must compile :domain, :data, :app
and run JVM/Android instrumentation tests, then device-specific regressions.
The release gate remains closed until source, independent security, device
and owner acceptance evidence are available.

## Still open without needing Google Cloud or Play Billing

Even after this milestone, the original account/email recovery and
full lossless data migration, native NPS/NSDL/CDSL verified statement
imports, complete interest/tax/loan parity, autonomous market refresh
subject to official data contracts, scheduled local encrypted backups,
full accessibility and multilingual polish, legal review, and real-device
penetration/performance testing remain open. Some are engineering tasks,
others depend on third-party formats, public-source access, user consent,
or acceptance evidence.

Google OAuth and Play Billing integration remain excluded from this
milestone on the owner's instruction. Neither is simulated.
