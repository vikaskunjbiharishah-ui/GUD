> **Working repository:** https://github.com/vikaskunjbiharishah-ui/GUD.git — branch `main` only. `shahvikask/kosha` is the frozen original reference, not the repository to commit to.

# M25 + M26 — pre-regression P0 recovery and family ownership hardening

**Source checkpoint:** 11 October 2026. **Development branch:** `main` only. **CI:** not yet invoked for this checkpoint, by explicit owner agreement. The exact combined regression is run once after all source review is complete.

## M25 — Forgotten local password without data loss

A native app password is a **separate local access-control verifier** (PBKDF2-HMAC-SHA256 with a Keystore non-exportable HMAC secret); it is *not* the SQLCipher vault data key. Consequently forgotten-password recovery must **never** clear private app data, regenerate the SQLCipher Keystore envelope, silently replace the DB, or substitute an expired/wrong device credential for a user secret.

Native local app login now provisions a **256-bit random, single-display recovery code** under the existing non-exportable Android Keystore HMAC key, while the original app password is verified and Android credential approval is fresh. Existing users with a known password are prompted to enroll their own recovery code on next login. No plaintext code is persisted; the code must be stored offline by the owner. The recovery screen is visible only when the local verifier is locked and the recovery tag has previously been enrolled.

Forgotten-password reset needs **both** the previously enrolled recovery code and a new Android secure-keyguard confirmation. Verification is constant-time, failures are persisted and rate-limited, and a successful reset rotates only the local password salt/tag, clears the old recovery tag to make the code **single use**, resets the local lock, and requires another ordinary login. No SQLCipher financial records, Android device vault wrapping keys, PAN, or legacy source archives are overwritten. Corrupt verifier metadata or missing Keystore keys fail closed. Device-only or security-answer-only resets are deliberately not offered.

**Recovery limitations:** This code cannot recover a missing Keystore, a destroyed database, a lost offline recovery code, or a previously enrolled user who forgot the password **before** provisioning a recovery code. For lost-device recovery, the owner must preserve a separately verified password-encrypted portable or historical vault backup and use a new installation. A historic `.kosha` password is not automatically equivalent to the native local app password.

## M25 — Native owner account registration / identity fields

After the primary Self family member exists, the authorized Family administration screen now offers **one-time native primary-owner profile registration**. It collects the original product's first/last name, email, 10-character PAN, DOB and two distinct questions from the original six question IDs. The original answer normalization and PBKDF2-SHA256 210,000-round independently salted verifiers are used; answers are never persisted in cleartext. The local Keystore-HMAC app-password verifier is **not exported or duplicated** as a weak password hash. Profile fields live exclusively inside the existing SQLCipher v4 `legacy_account_identity` backup-whitelisted record, with a `native_identity` marker and no exportable local-password verifier. Previously imported original accounts use the same backing table and cannot be silently overwritten by native signup. The visible record shows only a masked PAN and question IDs, never actual answers.

Registration stages an ephemeral command, obtains a NEW Android device confirmation and writes via the existing transactionally authorized repository. Mutable answer arrays are wiped on completion, failure, cancel and lock. Native encrypted portable backups retain the identity, while screenshots/recents are blocked by the existing `FLAG_SECURE`.

**Important security distinction:** An original security answer, PAN or date of birth by itself (or combined with Android device credential) is **not** granted permission to reset a local app password on an already-encrypted native vault. Such a bypass would weaken the new Keystore-bound password gate. Forgotten local passwords require the offline one-time recovery code **plus** OS device credential. Historic backup imports still require the two *original* security-answer proofs. Native login is still password-based, not email-username/password based; this is a source-parity difference requiring owner review before full M25 closure.

The native app now also enforces the original **180-day password-renewal age** at the authenticated UI boundary, using the previous-password + fresh OS challenge rotation flow. No financial data key is rotated during this local-password update.

## M25 — Original locked archive formats

The original `src/lib/backup.ts` accepts two encrypted formats:

- `GUDVLT01` binary: PBKDF2-HMAC-SHA256 600,000 rounds over `GrowUrDhan\nemail\npassword`, AES-256-GCM with 16-byte salt, 12-byte nonce, authenticated body.
- `KOSHA1` JSON: `{magic,saltB64,ivB64,cipherB64,iterations?}`, PBKDF2-HMAC-SHA256 over the source **password alone**, default 600,000 rounds, AES-256-GCM. The native implementation also permits the historical 210,000-round policy, but rejects other unsafe work factors. Inner version-1/2 vault JSON is normalized only for **missing optional** arrays; raw original row objects remain preserved in encrypted extension `extra_json`.

Both formats require separate verification of the embedded source email+password verifier, PAN, DOB and *both* original salted security answers. The same owner/custody/ledger correction, 1e8/1e4 integer precision, unknown-field and strict empty-target transaction rules apply. Unencrypted JSON exports are explicitly refused. Invalid owner/custody references and missing required identity attributes are not “repaired” by fabricating data.

**Synthetic instrumentation:** authenticated KOSHA1 preview/conversion into v4 SQLCipher, optional goal/CAS arrays, wrong source password and wrong security answer, foreign-key verification, nonempty-book denial and portable-backup roundtrip. Synthetic tests contain no real user's PDF, PAN or investment statement.

## M26 — Shared family selector and legal account ownership decision

A single **ephemeral owner scope** is shared from the top of the native shell across Home, Portfolio, family/accounts/goals, custody/holdings and CAS statement owner selection. It is removed when the vault leaves its verified session; no owner filter is persisted to unencrypted settings. Displays cannot change stored accounting ownership by selecting a different view.

**Decision:** preserve the **single legally accountable owner** `custody_account.owner_id` and composite holding/event FKs. The original `DematOrFolioAccount` source model, migration schema and existing v4 database support one owner per account. HUF and Joint remain selectable entity/relationship types for family record owners; a HUF's accounts belong to that explicit HUF member. A whole-family goal may reference holdings across family members. However, a single custody account is **not silently represented as jointly owned** by two independent members. True split-share or jointly legally owned account modelling would require a new explicit v5 schema with shares, beneficial owners and ledger allocation rules, plus migration and tax acceptance; it is not safe to fake with two copies of the same holding.

**Current scope:** native Home/Portfolio/Accounts/Family/Holdings/Statements share the selector; Tax and separately routed Documents as full native destinations have not yet been built and remain later milestone work. All database operations still recheck the actual owner, not the presentation filter.

## Device acceptance plan — outstanding, no pass claimed

No physical phone has been connected to this session, so **actual on-device approval is pending**. A build/emulator does not replace this gate.

1. New device: create native local password with Android lock, record and acknowledge recovery code. Force-stop/reopen, verify correct/wrong password; enter recovered code and new password; decline OS challenge then accept it; verify old password and one-time code both fail after success. Validate SQLCipher page integrity and all record counts unchanged.
2. Existing app on a phone with login already enrolled: log in normally, provision recovery code after OS confirmation, then test recovery. Confirm no code generated when OS challenge fails or Keystore key is unavailable.
3. Register the native primary owner's name/email/PAN/DOB and two distinct original security questions. Confirm aborting device challenge commits nothing; export and inspect encrypted portable backup with non-plaintext answer verifiers, then restore it. A second registration and an existing restored source account must not be overwritten.
4. Restore a **synthetic** authenticated `GUDVLT01` v2 and `KOSHA1` v1 backup into separate empty installations; compare owner IDs, HUF/guardian, custody, PAN mask, all 11 event kinds, supersessions, amounts, quantities, goal links/targets and CAS JSON before/after and after native portable backup roundtrip. Wrong identity, tampered ciphertext, duplicate file, occupied-vault imports must leave rows unchanged.
5. With two owners (Self/HUF/Joint relation), switch family scope across Home/Portfolio/Accounts/Goals/Holdings/Statements. Verify filtering never changes underlying ledger, values or account owner; cross-owner account move and tags must fail. Test inactive member with dependants and history; retry after restart.
6. Test low-memory activity recreation, orientation, Android app background, no screenshots/recents, screen-reader labels and small handset vs tablet form scroll; recovery-code screen must not expose a saved-state secret.
7. Record device/API/Android security patch, test operator, sanitized fixture hashes, SQLCipher FK/integrity results and pass/fail in M34 release report.

## CI gate

After the source implementation reaches review acceptance, trigger **one** exact-SHA regression on final `main`, covering existing M25 tests, the new KOSHA1 tests and local-password instrumentation, M26 Kotlin/JVM and Android 35 SQLCipher/Compose tests, source security preflight, v1–v4 database upgrades, and debug + minified release AAB. Do not change `.github/ci-trigger/native-regression.txt` until approved; untested source must not be called a green build.

Issue #3 (M25) and #4 (M26) remain OPEN until code-review, corrected synthetic CI and the essential device signoff are completed.
