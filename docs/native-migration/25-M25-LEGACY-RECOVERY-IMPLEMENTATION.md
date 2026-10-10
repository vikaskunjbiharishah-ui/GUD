# M25 — Original locked-vault recovery into native SQLCipher (source implementation)

**Date:** 2026-10-10  
**Parent:** #1, tracked in #3  
**Original contract:** `shahvikask/kosha@af96dcc`, `src/lib/{backup,auth,money}.ts` and `src/types.ts`.  
**Native branch:** `milestone/m25-legacy-recovery` (based on M24 corrective `7723cc9`).  
**Status:** SOURCE IMPLEMENTED for verified v2 `.kosha` import into **empty** native vault; CI/physical acceptance and remaining original-login parity **OPEN**.

## Scope implemented

- Legacy file must have `GUDVLT01` marker, exact 600,000-round PBKDF2-SHA256 envelope, AES-256-GCM ciphertext and v2 `GUDVAULT` inner JSON.
- Full independent original account recovery gate: verify normalized source email+password, embedded account PBKDF2-SHA256 verifier at original 210,000 rounds, full PAN or authentic original masked PAN (ABCDE****F), exact DOB, two distinct matching security-question IDs and normalized answers against original independent salted PBKDF2 hashes.
- Native local-password admission and **fresh** Android device credential still required. A source backup is **not** a bypass for an existing native-password lock or missing Android Keystore.
- Original source remains unchanged. SAF file bytes and recovery passwords are consumed on-device, bounded to 32 MiB, and wiped from mutable arrays on all normal/failure paths.
- Transactional conversion to native v4 SQLCipher logical backup table layout: family members/profile, registrar/custody accounts/profiles, holdings/profiles, events/append-only ledger profile, goals, original account verifier and the raw CAS snapshot. Fields that the native screens do not yet interpret are preserved as raw JSON in their corresponding `extra_json` column.
- Original money minor and unit quantities are the **same scaled integers** as native (1e4 INR and 1e8 units): strict string-to-long conversions; no floating-point rounding or invented BUY records.
- Existing 11 original event kinds keep their exact identifiers and event dates. Legacy predecessor/superseding corrections are placed in dependency order; bad references or cross-owner records abort before database writes.
- Historical imported CAS `casLatest` JSON is retained in encrypted `vault_meta` but is **not automatically replayed** into the append-only ledger or marketed as verified statement imports.
- Goals with absent original target amounts retain nullable amounts, not fictitious zero targets.
- Native restore is **empty-vault only**. It rejects any book that contains existing records, never merges, and uses one SQLCipher transaction with rollback and native FKs. A formerly encrypted vault with missing Keystore key is NOT silently reset.
- Converted v4 native backups contain the original account, holdings, transaction metadata and CAS metadata. Native portable backups remain independently password-encrypted.

## Exact implemented source locations

`data/vault/LegacyVaultMigration.kt`: source authentication + schema-validated conversion  
`domain/vault/LegacyRestoreIdentity.kt`: ephemeral recovery identity with wiping  
`domain/vault/VaultRepository.kt` and `data/vault/SqlCipherVaultRepository.kt`: explicitly authorized, atomic importer  
`data/vault/VaultBackupTables.kt`: shared whitelist and conversion document  
`data/vault/VaultSchema.kt`: nullable legacy goal target representation  
`app/presentation/screens/LegacyBackupScreen.kt` and `app/MainActivity.kt`: original backup preview, full recovery questions, OS credential, bounded SAF read, restore result  
`data/androidTest/.../LegacyVaultMigrationInstrumentedTest.kt`: synthetic end-to-end success and denial cases  
`app/androidTest/.../LegacyBackupShellTest.kt`: restore entry and identity-question UI

## Security decisions and limitations

1. Source archive password and original account identity must *both* be verified, followed by the authenticated native + Android-device gate. A simple email match is insufficient.
2. Only original binary locked v2 `.kosha` archives are supported. The historical text `KOSHA1` envelope and unlocked JSON are **not** accepted by the new importer.
3. Data is preserved in SQLCipher but many native M26–M31 editing, tax, dashboard and issuer screens have not been implemented; restored raw field metadata is not yet functionally editable.
4. Native local app-password **forgotten-password reset is not implemented**. That cannot safely bypass Android Keystore or replace a hardware-bound vault key based solely on user-entered security answers. Original new-account profile registration, expiration-warning parity and offline recovery UX still require further development.
5. If the original archive contains invalid IDs, duplicate rows, dangling/cross-family references, unsupported root collections, impossible dates or costs, the importer refuses and preserves the source. It does not silently normalize or discard data. Root collection allowlist: `kind`, `version`, `exportedAt`, `account`, `familyMembers`, `holdings`, `ledgerEvents`, `accounts`, `goals`, `casLatest`.
6. Source metadata (including optionally stored full PAN and password *hashes*, not plaintext passwords) remains encrypted by SQLCipher and included in user-encrypted portable native backups; do not export unencrypted recovery reports or add logging.
7. If native schema 4 data already exists, a merge is forbidden. Install into a clean vault or restore to a separate fresh device; do not clear app data on a live financial phone.
8. This is a code implementation, not a signoff for existing user's real archive; use synthetic files, not real PAN or PDF, in CI.

## Test requirements before any completion marker

- Python v1/2/3 to v4 preflight and original-source parity tests.
- JVM and Compose compilation on exact SHA.
- Android API35 SQLCipher instrumentation, synthetic authenticated old-vault fixture, wrong password/PAN/DOB/security answers, tampering, correction graph, cross-owner references, source event counts, nullable target, original CAS JSON, rollback on nonempty book, portable backup roundtrip and source archival hash preservation.
- Physical phone trial with anonymized/owner-provided **encrypted** v2 archive, existing-vault nonoverwrite, no hidden network, exported backup across new Android Keystore and real asset metadata reconciliation.
- Full original app login/forgot-password and any required KOSHA1-v1 conversion plus accessibility UX before marking **M25 COMPLETE**.

**No proof is claimed from a GitHub commit alone.** The milestone remains OPEN until its P0 features and exact-SHA/physical regression pass.
