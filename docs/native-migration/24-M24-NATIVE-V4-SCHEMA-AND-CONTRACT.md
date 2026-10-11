# M24 — React v9 → Native SQLCipher v4 data-contract foundation

Date: 10 October 2026  
Target: `vikaskunjbiharishah-ui/GUD`, isolated `milestone/m24-native-parity-contract`  
Original (read-only): `shahvikask/kosha@af96dcc8967ab595e1d9acee78e29e500cb99a9e` / `GUD-Android-Gmail`  
Base native SHA: `721cd39a05409842fae462800e436cc4f417295b`  
Issue: [M24 #2](https://github.com/vikaskunjbiharishah-ui/GUD/issues/2)

## Status and boundaries

M24 adds a **persistence contract**, not complete React feature parity, UI CRUD, or an automatic legacy restore. The running React/Tauri application uses schema v9; the new native SQLCipher schema advances **from v3 to v4**. The two schema version numbers are independent. The existing production Android native vault still uses Keystore-wrapped SQLCipher; the former React/WebView snapshot is *not* used by the native app.

The new schema intentionally extends v1–v3 tables rather than copying a v9 SQLite database into SQLCipher or mutating append-only records. Original `.kosha` credentials, identity verification and full migration belong to M25 and must not be claimed to work because these tables exist. Nothing writes real account/PAN/backup data during this milestone.

## Contract source audit

Original sources: `src/db/schema.ts` (v9), `src/types.ts`, `src/context/KoshaContext.tsx`, `src/lib/backup.ts`, `src/lib/lots.ts`. Every original source file is present unchanged in the isolated GUD repo, checked against the reference blob hash.

Original `AssetCategory` has exactly **eight** values: EQUITY, MUTUAL_FUND, GOLD_SGB, FIXED_INCOME, REAL_ESTATE, SPECIALISED, CASH_BANK and LIABILITY. Original `EventType` has exactly **11**: OPENING_BALANCE, SNAPSHOT_ADJUSTMENT, BUY, SELL, BONUS, SPLIT, DIVIDEND, INTEREST, VALUATION_UPDATE, REPAYMENT, CORRECTION. Original `HoldingDetails` contains **36** optional asset-specific keys. The new `ReactV9ParityContract.kt` freezes these source values and the automatic source-level tests compare the sets directly.

## Original-data field mapping strategy

| React v9 record and principal fields | Native v4 storage | Safety interpretation |
| --- | --- | --- |
| `entity`: type, PAN, date of birth/majority, guardian, color, CAS ID, PRAN, Gmail flags/cursors | Existing `family_member` + `family_profile` | Locally SQLCipher-encrypted; all ownership matched by owner ID |
| `account`: custodian, full/masked identifier, DP/client/BO, nominee, currency, opening/closing | Existing `custody_account` + `custody_profile` | Composite account/owner FK prevents account mismatch |
| `instrument`: taxonomy, ISIN, exchange, advisor/AMC/RTA, allocation, grandfathering | `instrument_profile` | Instrument IDs maintained for future cross-holding mapping |
| `holding`: price history/provenance, cost, SGB/EMI terms, tags/goals, `details_json` | Existing `holding` + `holding_profile` | NULL historical cost means UNKNOWN, never synthetic zero |
| `ledger_event`: original 11 kinds, corrections, fee, notes, source, date, batch | Existing immutable ledger + immutable `ledger_profile` | No event rewritten; owner/holding pair checked on insert |
| `goal` and tags | `family_goal`, `holding_goal` | Shared/owner-scoped goals checked by trigger |
| `import_batch`, `source_document`, CAS history | `import_batch_profile`, `source_document_profile`, `statement_snapshot_profile`, `statement_position_profile` | Provenance and nullable invested/cost; PDF bytes not stored |
| `price_observation`, `listed_security` | `price_observation_profile`, `listed_security_profile` | Scaled integer prices, source and as-of |
| `tax_rule_set`, `liability`, `setting` | `tax_rule_profile`, `liability_profile`, `setting_profile` | Tax rule metadata immutable; scaled integer money |
| `vault_account`, `gmail_seen`, `egress_log` | `legacy_account_identity`, `gmail_seen_profile`, `egress_log_profile` | Source-verifier metadata only; no OAuth tokens in migration tables |
| Source-v9 field not yet typed in native | `legacy_field_extension` (JSON with source version) | Reject or preserve losslessly during future M25 import; no silent discard |

Original optional nested `HoldingDetails` JSON (36 fields) remains opaque until M25 parsing; this preserves its exact serialized contents without unreviewed conversions from floating percentages into scaled values. The native core has an explicit nullable acquisition-cost type to distinguish actual zero from unknown.

## M24 migration and backup invariants

1. Fresh installs execute v1 SQL + v2 financial fields + v3 CAS identity + v4 parity extension inside ONE SQLCipher transaction. Existing v1, v2 and v3 books upgrade only missing steps. `PRAGMA user_version` becomes **4** only after success.
2. v4 adds 20 extension tables and six triggers to immutable base schema. It does not drop/rebuild `ledger_event`, reprice holdings, reassign family ownership or synthesize purchase events.
3. Opening checks source tables, immutable ledger/CAS/goal/tax triggers, cipher integrity, logical integrity and foreign keys. Forged version numbers with missing safety objects fail closed.
4. Portable **native** logical backup format advances from 3 to **4** and whitelists all new tables and their scalar columns (INTEGER stored as decimal strings). Native formats 1, 2 and 3 remain explicit restore inputs with extensions left empty; merging into a non-empty book remains prohibited.
5. `VaultBackupCodec` still wraps logical backup bytes with PBKDF2+AES-GCM; original `GUDVLT01` `.kosha` restoration is not implemented in M24.
6. Native SQLCipher, Keystore, transaction authorization, hashed source records, owner filters and append-only correction rules must remain in effect.

## Checks included (not yet executed on new SHA)

- `android-native/scripts/validate_schema.py`: parse real Kotlin SQL, v1→v2→v3→v4 and v3→v4, partial-migration rollback, fresh vault v4, FK and same-owner checks, preserved original event, immutable metadata/tax rules, nullable cost and integer-only money.
- `android-native/scripts/test_react_v9_contract.py`: source-level exact equality for categories, event kinds, all 36 `HoldingDetails` fields, required extension tables, backup whitelist and v3 backwards acceptance. Discovered by existing unit-script workflow.
- `android-native/data/src/androidTest/java/in/growurdhan/data/vault/NativeParitySchemaInstrumentedTest.kt`: v3 SQLCipher in-place upgrade without ledger mutation, native v4 encrypted-backup logical round-trip, no unintended field loss.
- Existing Android/JVM suites remain the regression gate. No current-commit Gradle/emulator or real device run has been performed by creating these files.

## Acceptance still required

- Run `python3 android-native/scripts/validate_schema.py` and `python3 -m unittest discover -s android-native/scripts -p 'test_*.py' -v`; run native :core:model/:domain/:data/:app Gradle and device tests on the **exact M24 SHA** (single deliberate CI job).
- Confirm migration on a real SQLCipher **v3 vault image** and repeat open/backup/restore and corrupted vault rejection; do not use a production wealth vault as a test fixture.
- Complete M25's original `.kosha` authentication + lossless import mapping before calling existing-user migration complete.
- Treat creation of tables as SOURCE IMPLEMENTED only. The M26–M33 screens, data writes, financial workflows, Gmail, pricing and Play Billing are NOT delivered by M24.

Release gate remains CLOSED until user/device/security and external commercial requirements are accepted.
