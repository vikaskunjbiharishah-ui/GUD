package `in`.growurdhan.data.vault

import net.zetetic.database.sqlcipher.SQLiteDatabase

/** Native schema v5; rollback-safe upgrades v1–v4 without rewriting immutable ledger rows. */
internal object VaultSchema {
    const val VERSION = 5

    fun initializeOrValidate(db: SQLiteDatabase) {
        // SQLCipher returns a result row for secure_delete even when setting it.
        // execSQL routes to executeForChangedRowCount and rejects result-producing
        // PRAGMAs on Android. Execute it through a query cursor and check its result.
        db.execSQL("PRAGMA foreign_keys = ON")
        db.rawQuery("PRAGMA foreign_keys", null).use { cursor ->
            check(cursor.moveToFirst() && cursor.getInt(0) == 1) {
                "SQLite foreign-key enforcement could not be enabled."
            }
        }
        db.rawQuery("PRAGMA secure_delete = ON", null).use { cursor ->
            check(cursor.moveToFirst() && cursor.getInt(0) == 1) {
                "SQLite secure deletion could not be enabled."
            }
        }
        val current = db.rawQuery("PRAGMA user_version", null).use { cursor ->
            check(cursor.moveToFirst()) { "Missing SQLite user_version." }
            cursor.getInt(0)
        }
        when (current) {
            in 0..4 -> {
                // v1–v4 -> v5, and fresh installs, are one rollback-safe transaction.
                db.beginTransaction()
                try {
                    if (current == 0) for (sql in ddl) db.execSQL(sql)
                    if (current <= 1) for (sql in migrationToV2) db.execSQL(sql)
                    if (current <= 2) for (sql in migrationToV3) db.execSQL(sql)
                    if (current <= 3) for (sql in migrationToV4) db.execSQL(sql)
                    if (current <= 4) for (sql in migrationToV5) db.execSQL(sql)
                    db.execSQL("PRAGMA user_version = $VERSION")
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
            VERSION -> Unit
            else -> throw VaultUnavailable("Unsupported native vault schema: $current.")
        }
        // Fail closed if a forged or incomplete version number omits required columns.
        val columns = db.rawQuery("PRAGMA table_info(ledger_event)", null).use { cursor ->
            buildSet { while (cursor.moveToNext()) add(cursor.getString(1)) }
        }
        check(columns.containsAll(setOf(
            "financial_kind", "unit_price_scaled", "charges_scaled", "supersedes_event_id",
        ))) { "Native ledger schema v2 is incomplete." }
        val mappingColumns = db.rawQuery("PRAGMA table_info(holding_cas_identity)", null).use { cursor ->
            buildSet { while (cursor.moveToNext()) add(cursor.getString(1)) }
        }
        check(mappingColumns.containsAll(setOf(
            "holding_id", "owner_id", "account_id", "isin", "folio",
        ))) { "Native CAS identity table is missing or incomplete." }
        val profileColumns = db.rawQuery("PRAGMA table_info(ledger_profile)", null).use { cursor ->
            buildSet { while (cursor.moveToNext()) add(cursor.getString(1)) }
        }
        check(profileColumns.containsAll(setOf(
            "settlement_date", "unit_balance_after", "description", "entered_by",
        ))) { "Native ledger profile is missing the legacy event columns." }
        // The extension tables preserve original React/Tauri fields without replacing
        // existing native model tables, financial rows or v1 ledger triggers.
        val parityTables = setOf(
            "family_profile", "custody_profile", "instrument_profile", "holding_profile",
            "ledger_profile", "family_goal", "holding_goal", "import_batch_profile",
            "source_document_profile", "statement_snapshot_profile",
            "statement_position_profile", "price_observation_profile", "tax_rule_profile",
            "liability_profile", "legacy_account_identity", "legacy_field_extension",
            "listed_security_profile", "egress_log_profile", "gmail_seen_profile",
            "setting_profile",
        )
        val actualTables = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use {
            buildSet { while (it.moveToNext()) add(it.getString(0)) }
        }
        check(actualTables.containsAll(parityTables)) {
            "Native v4 parity contract tables are incomplete."
        }
        // A copied/forged user_version must never bypass immutable ledger or
        // enrolled-identity authorization triggers.
        val requiredTriggers = setOf(
            "deny_ledger_update", "deny_ledger_delete", "validate_rich_ledger_insert",
            "cas_holding_account_match", "deny_cas_mapping_update", "deny_cas_mapping_delete",
            "validate_ledger_profile_owner", "deny_ledger_profile_update",
            "deny_ledger_profile_delete", "validate_ledger_profile_settlement",
            "validate_holding_goal_owner",
            "deny_tax_rule_profile_update", "deny_tax_rule_profile_delete",
        )
        val triggerNames = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='trigger'", null,
        ).use { cursor ->
            buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        check(triggerNames.containsAll(requiredTriggers)) {
            "Native vault safety triggers are incomplete."
        }
        // SQLCipher's PRAGMA cipher_integrity_check verifies each encrypted
        // page HMAC independently of SQLite's logical integrity_check.
        // Per the SQLCipher API, zero result rows means success; one or more
        // rows indicates damaged or unauthenticated pages. Validate on EVERY
        // connection open, including Activity-driven PDF and backup workflows.
        val pageErrors = db.rawQuery("PRAGMA cipher_integrity_check", null).use { it.count }
        if (pageErrors != 0) {
            throw VaultUnavailable("Encrypted vault page authentication failed.")
        }
        val integrity = db.rawQuery("PRAGMA integrity_check", null).use {
            if (it.moveToFirst()) it.getString(0) else "unavailable"
        }
        if (integrity != "ok") throw VaultUnavailable("Encrypted vault integrity verification failed.")
        val foreignKeys = db.rawQuery("PRAGMA foreign_key_check", null).use { it.count }
        if (foreignKeys != 0) throw VaultUnavailable("Encrypted vault references are inconsistent.")
    }

    /**
     * Stored legacy kind remains constrained by v1 CHECK. Rich event kind is explicit,
     * with ADJUSTMENT as the legacy carrier for BONUS/SPLIT/other added event types.
     * This avoids rebuilding the append-only table and preserves old rows and triggers.
     * The insert validation trigger prevents inconsistent carriers or forged correction links.
     */
    internal val migrationToV2 = listOf(
        """ALTER TABLE ledger_event ADD COLUMN financial_kind TEXT NOT NULL DEFAULT ''
           CHECK(financial_kind IN ('', 'BUY','SELL','DIVIDEND','INTEREST',
               'OPENING_BALANCE','ADJUSTMENT','CORRECTION','BONUS','SPLIT',
               'SNAPSHOT_ADJUSTMENT','VALUATION_UPDATE','REPAYMENT'))""",
        "ALTER TABLE ledger_event ADD COLUMN unit_price_scaled INTEGER",
        "ALTER TABLE ledger_event ADD COLUMN charges_scaled INTEGER NOT NULL DEFAULT 0 CHECK(charges_scaled >= 0)",
        "ALTER TABLE ledger_event ADD COLUMN supersedes_event_id TEXT",
        "CREATE UNIQUE INDEX ledger_unique_supersession ON ledger_event(supersedes_event_id) WHERE supersedes_event_id IS NOT NULL",
        """
        CREATE TRIGGER validate_rich_ledger_insert BEFORE INSERT ON ledger_event
        WHEN NEW.financial_kind != '' OR NEW.supersedes_event_id IS NOT NULL
        BEGIN
          SELECT CASE WHEN
            (NEW.financial_kind IN ('BONUS','SPLIT','SNAPSHOT_ADJUSTMENT','VALUATION_UPDATE','REPAYMENT')
              AND NEW.kind != 'ADJUSTMENT') OR
            (NEW.financial_kind NOT IN ('BONUS','SPLIT','SNAPSHOT_ADJUSTMENT','VALUATION_UPDATE','REPAYMENT')
              AND NEW.financial_kind != '' AND NEW.kind != NEW.financial_kind)
            THEN RAISE(ABORT, 'Financial event kind does not match ledger carrier') END;
          SELECT CASE WHEN
            NEW.financial_kind = 'CORRECTION' AND NEW.supersedes_event_id IS NULL
            THEN RAISE(ABORT, 'A correction must supersede an existing event') END;
          SELECT CASE WHEN
            NEW.supersedes_event_id IS NOT NULL AND NEW.financial_kind != 'CORRECTION'
            THEN RAISE(ABORT, 'Only a correction can supersede an event') END;
          SELECT CASE WHEN
            NEW.supersedes_event_id IS NOT NULL AND NOT EXISTS (
              SELECT 1 FROM ledger_event AS prior
              WHERE prior.id = NEW.supersedes_event_id AND prior.owner_id = NEW.owner_id
                AND prior.holding_id = NEW.holding_id
            ) THEN RAISE(ABORT, 'Missing or cross-owner correction predecessor') END;
        END
        """.trimIndent(),
    )

    /**
     * The identity link is a separate table so old holding and ledger rows
     * migrate unchanged. Composite ownership FKs + insert trigger deny
     * mismatched account/owner/holding bindings.
     */
    internal val migrationToV3 = listOf(
        """
        CREATE TABLE holding_cas_identity (
          holding_id TEXT PRIMARY KEY NOT NULL,
          owner_id TEXT NOT NULL,
          account_id TEXT NOT NULL,
          isin TEXT NOT NULL CHECK(length(isin) = 12 AND isin = upper(isin)),
          folio TEXT NOT NULL CHECK(length(folio) BETWEEN 1 AND 64),
          UNIQUE(owner_id, isin, folio),
          FOREIGN KEY(holding_id, owner_id) REFERENCES holding(id, owner_id) ON DELETE RESTRICT,
          FOREIGN KEY(account_id, owner_id) REFERENCES custody_account(id, owner_id) ON DELETE RESTRICT
        )
        """.trimIndent(),
        """
        CREATE TRIGGER cas_holding_account_match BEFORE INSERT ON holding_cas_identity
        BEGIN
          SELECT CASE WHEN NOT EXISTS (
            SELECT 1 FROM holding
            WHERE id=NEW.holding_id AND owner_id=NEW.owner_id
              AND account_id=NEW.account_id
              AND asset_category='MUTUAL_FUND'
          ) THEN RAISE(ABORT, 'CAS mapping requires matching account, owner and mutual fund') END;
          SELECT CASE WHEN NOT EXISTS (
            SELECT 1 FROM custody_account
            WHERE id=NEW.account_id AND owner_id=NEW.owner_id
              AND account_type IN ('CAMS','KFINTECH')
          ) THEN RAISE(ABORT, 'CAS mapping requires matching registrar account') END;
        END
        """.trimIndent(),
        """CREATE TRIGGER deny_cas_mapping_update BEFORE UPDATE ON holding_cas_identity
             BEGIN SELECT RAISE(ABORT, 'CAS identity is immutable'); END""",
        """CREATE TRIGGER deny_cas_mapping_delete BEFORE DELETE ON holding_cas_identity
             BEGIN SELECT RAISE(ABORT, 'CAS identity is immutable'); END""",
    )

    /**
     * M24 parity-only extension. Keep existing v1-v3 tables and append-only ledger intact;
     * unknown historical acquisition cost is NULL (not zero or fabricated).
     * The records are part of SQLCipher, not a sidecar or unencrypted migration cache.
     * M25 will implement audited account-vault ingestion, not automatically import data here.
     */
    internal val migrationToV4 = listOf(
        """
        CREATE TABLE family_profile (
          owner_id TEXT PRIMARY KEY NOT NULL REFERENCES family_member(id) ON DELETE RESTRICT,
          entity_type TEXT NOT NULL DEFAULT 'INDIVIDUAL'
            CHECK(entity_type IN ('INDIVIDUAL','HUF','PROPRIETORSHIP','PRIVATE_TRUST','PARTNERSHIP_FIRM')),
          pan_masked TEXT, pan_for_statements TEXT, date_of_birth TEXT, date_of_majority TEXT,
          guardian_id TEXT REFERENCES family_member(id) ON DELETE RESTRICT,
          color TEXT, notes TEXT, is_active INTEGER NOT NULL DEFAULT 1 CHECK(is_active IN (0,1)),
          created_at TEXT, cas_id TEXT, pran TEXT,
          gmail_sync INTEGER CHECK(gmail_sync IN (0,1)), cas_id_source TEXT,
          pran_source TEXT, cas_sync_after TEXT, pran_sync_after TEXT, extra_json TEXT
        )
        """.trimIndent(),
        """
        CREATE TABLE custody_profile (
          account_id TEXT NOT NULL, owner_id TEXT NOT NULL,
          custodian TEXT, identifier_full TEXT, dp_id TEXT, client_id TEXT, bo_id TEXT,
          base_currency TEXT NOT NULL DEFAULT 'INR', nominee TEXT,
          opened_on TEXT, closed_on TEXT, created_at TEXT, extra_json TEXT,
          PRIMARY KEY(account_id, owner_id),
          FOREIGN KEY(account_id,owner_id) REFERENCES custody_account(id,owner_id) ON DELETE RESTRICT
        )
        """.trimIndent(),
        """
        CREATE TABLE instrument_profile (
          id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL,
          sub_category TEXT, symbol_or_folio TEXT, isin TEXT, currency TEXT NOT NULL DEFAULT 'INR',
          exposure_node TEXT, l2_node TEXT, wrapper TEXT, allocation_split_json TEXT,
          fmv_31jan2018_scaled INTEGER, amc TEXT, rta TEXT, amfi_code TEXT,
          advisor TEXT, scheme_type TEXT, ucc TEXT, exchange TEXT, dp_name TEXT,
          is_user_created INTEGER NOT NULL DEFAULT 1 CHECK(is_user_created IN (0,1)),
          created_at TEXT, extra_json TEXT
        )
        """.trimIndent(),
        """
        CREATE TABLE holding_profile (
          holding_id TEXT NOT NULL, owner_id TEXT NOT NULL,
          instrument_id TEXT REFERENCES instrument_profile(id) ON DELETE RESTRICT,
          symbol_or_folio TEXT, isin TEXT, sub_category TEXT,
          average_cost_per_unit_scaled INTEGER, cost_unknown INTEGER NOT NULL DEFAULT 1 CHECK(cost_unknown IN (0,1)),
          benchmark_price_scaled INTEGER, issue_price_scaled INTEGER,
          coupon_rate_bps INTEGER, next_coupon_date TEXT, maturity_date TEXT,
          interest_rate_bps INTEGER, institution_name TEXT, account_number_masked TEXT,
          monthly_emi_scaled INTEGER, emi_status TEXT, remaining_emi_count INTEGER,
          acquired_on TEXT, details_json TEXT, last_price_updated TEXT, is_price_stale INTEGER CHECK(is_price_stale IN (0,1)),
          price_source TEXT, needs_review INTEGER NOT NULL DEFAULT 0 CHECK(needs_review IN (0,1)),
          review_reasons_json TEXT, fmv_31jan2018_scaled INTEGER, exposure_node TEXT,
          l2_node TEXT, wrapper TEXT, allocation_split_json TEXT,
          notes TEXT, location TEXT, tag TEXT, tags_json TEXT, extra_json TEXT,
          PRIMARY KEY(holding_id, owner_id),
          FOREIGN KEY(holding_id,owner_id) REFERENCES holding(id,owner_id) ON DELETE RESTRICT
        )
        """.trimIndent(),
        """
        CREATE TABLE ledger_profile (
          event_id TEXT PRIMARY KEY NOT NULL REFERENCES ledger_event(id) ON DELETE RESTRICT,
          owner_id TEXT NOT NULL, holding_id TEXT NOT NULL,
          holding_name TEXT, account_id TEXT, import_batch_id TEXT, source_ref TEXT,
          notes TEXT, legacy_created_at TEXT, extra_json TEXT,
          FOREIGN KEY(holding_id,owner_id) REFERENCES holding(id,owner_id) ON DELETE RESTRICT
        )
        """.trimIndent(),
        """
        CREATE TRIGGER validate_ledger_profile_owner BEFORE INSERT ON ledger_profile
        BEGIN
          SELECT CASE WHEN NOT EXISTS (
            SELECT 1 FROM ledger_event WHERE id=NEW.event_id AND owner_id=NEW.owner_id
              AND holding_id=NEW.holding_id
          ) THEN RAISE(ABORT,'Ledger profile owner and event must match') END;
        END
        """.trimIndent(),
        """
        CREATE TRIGGER deny_ledger_profile_update BEFORE UPDATE ON ledger_profile
        BEGIN SELECT RAISE(ABORT,'Ledger metadata is append only'); END
        """.trimIndent(),
        """
        CREATE TRIGGER deny_ledger_profile_delete BEFORE DELETE ON ledger_profile
        BEGIN SELECT RAISE(ABORT,'Ledger metadata is append only'); END
        """.trimIndent(),
        """
        CREATE TABLE family_goal (
          id TEXT PRIMARY KEY NOT NULL,
          owner_id TEXT REFERENCES family_member(id) ON DELETE RESTRICT,
          name TEXT NOT NULL CHECK(length(name) BETWEEN 1 AND 300),
          target_scaled INTEGER CHECK(target_scaled >= 0),
          target_date TEXT, priority TEXT, created_at TEXT NOT NULL, extra_json TEXT
        )
        """.trimIndent(),
        """
        CREATE TABLE holding_goal (
          goal_id TEXT NOT NULL REFERENCES family_goal(id) ON DELETE RESTRICT,
          holding_id TEXT NOT NULL, owner_id TEXT NOT NULL,
          PRIMARY KEY(goal_id,holding_id),
          FOREIGN KEY(holding_id,owner_id) REFERENCES holding(id,owner_id) ON DELETE RESTRICT
        )
        """.trimIndent(),
        """
        CREATE TRIGGER validate_holding_goal_owner BEFORE INSERT ON holding_goal
        BEGIN
          SELECT CASE WHEN NOT EXISTS (
            SELECT 1 FROM family_goal WHERE id=NEW.goal_id AND
              (owner_id IS NULL OR owner_id=NEW.owner_id)
          ) THEN RAISE(ABORT,'Goal must be household-wide or belong to holding owner') END;
        END
        """.trimIndent(),
        """
        CREATE TABLE import_batch_profile (
          id TEXT PRIMARY KEY NOT NULL,
          owner_id TEXT REFERENCES family_member(id) ON DELETE RESTRICT,
          filename TEXT, kind TEXT, statement_date TEXT, period_from TEXT, period_to TEXT,
          investor_name TEXT, investor_pan_masked TEXT, investor_email TEXT, cas_id TEXT,
          statement_type TEXT, rows_new INTEGER CHECK(rows_new >= 0),
          rows_conflict INTEGER CHECK(rows_conflict >= 0),
          rows_duplicate INTEGER CHECK(rows_duplicate >= 0),
          committed_at TEXT, source_sha256 TEXT, extra_json TEXT
        )
        """.trimIndent(),
        """
        CREATE TABLE source_document_profile (
          id TEXT PRIMARY KEY NOT NULL,
          batch_id TEXT REFERENCES import_batch_profile(id) ON DELETE RESTRICT,
          owner_id TEXT REFERENCES family_member(id) ON DELETE RESTRICT,
          issuer TEXT, filename TEXT, document_kind TEXT, mime_type TEXT,
          source_sha256 TEXT, byte_length INTEGER, page_count INTEGER, received_at TEXT,
          extra_json TEXT
        )
        """.trimIndent(),
        """
        CREATE TABLE statement_snapshot_profile (
          snapshot_id TEXT NOT NULL, owner_id TEXT NOT NULL,
          issuer TEXT, period_from TEXT, period_to TEXT,
          import_batch_id TEXT REFERENCES import_batch_profile(id) ON DELETE RESTRICT,
          investor_name TEXT, invested_scaled INTEGER, extra_json TEXT,
          PRIMARY KEY(snapshot_id,owner_id),
          FOREIGN KEY(snapshot_id,owner_id) REFERENCES snapshot_import(id,owner_id) ON DELETE RESTRICT
        )
        """.trimIndent(),
        """
        CREATE TABLE statement_position_profile (
          snapshot_id TEXT NOT NULL, owner_id TEXT NOT NULL, holding_id TEXT NOT NULL,
          invested_scaled INTEGER, source_line_id TEXT, statement_date TEXT,
          unit_price_scaled INTEGER, isin TEXT, folio TEXT, current_value_scaled INTEGER,
          amc TEXT, scheme_type TEXT, source_section TEXT, extra_json TEXT,
          PRIMARY KEY(snapshot_id,holding_id),
          FOREIGN KEY(snapshot_id,owner_id) REFERENCES snapshot_import(id,owner_id) ON DELETE RESTRICT,
          FOREIGN KEY(holding_id,owner_id) REFERENCES holding(id,owner_id) ON DELETE RESTRICT
        )
        """.trimIndent(),
        """
        CREATE TABLE price_observation_profile (
          id TEXT PRIMARY KEY NOT NULL,
          instrument_id TEXT REFERENCES instrument_profile(id) ON DELETE RESTRICT,
          holding_id TEXT, owner_id TEXT,
          as_of TEXT NOT NULL, price_scaled INTEGER NOT NULL CHECK(price_scaled >= 0),
          basis TEXT, currency TEXT NOT NULL DEFAULT 'INR',
          source TEXT NOT NULL, recorded_at TEXT, extra_json TEXT,
          FOREIGN KEY(holding_id,owner_id) REFERENCES holding(id,owner_id) ON DELETE RESTRICT,
          CHECK ((holding_id IS NULL AND owner_id IS NULL) OR
                 (holding_id IS NOT NULL AND owner_id IS NOT NULL))
        )
        """.trimIndent(),
        """
        CREATE TABLE tax_rule_profile (
          id TEXT PRIMARY KEY NOT NULL, effective_from TEXT, fiscal_year TEXT,
          assessment_year TEXT, rules_json TEXT NOT NULL, source TEXT,
          published_at TEXT, installed_at TEXT
        )
        """.trimIndent(),
        """
        CREATE TABLE liability_profile (
          holding_id TEXT NOT NULL, owner_id TEXT NOT NULL,
          lender TEXT, liability_type TEXT, name TEXT, principal_scaled INTEGER,
          outstanding_scaled INTEGER, monthly_emi_scaled INTEGER,
          rate_type TEXT, interest_rate_bps INTEGER, remaining_emi_count INTEGER,
          start_date TEXT, end_date TEXT, created_at TEXT,
          linked_holding_id TEXT, details_json TEXT,
          PRIMARY KEY(holding_id,owner_id),
          FOREIGN KEY(holding_id,owner_id) REFERENCES holding(id,owner_id) ON DELETE RESTRICT
        )
        """.trimIndent(),
        """
        CREATE TABLE legacy_account_identity (
          id INTEGER PRIMARY KEY CHECK(id=1),
          email TEXT, first_name TEXT, last_name TEXT, pan_masked TEXT,
          pan_for_statements TEXT, date_of_birth TEXT, password_salt TEXT,
          password_hash TEXT, password_iterations INTEGER, password_changed_at TEXT,
          security_q1_id TEXT, security_q1_hash TEXT,
          security_q2_id TEXT, security_q2_hash TEXT, created_at TEXT, extra_json TEXT
        )
        """.trimIndent(),
        """
        CREATE TABLE legacy_field_extension (
          record_type TEXT NOT NULL, record_id TEXT NOT NULL,
          owner_id TEXT REFERENCES family_member(id) ON DELETE RESTRICT,
          field_name TEXT NOT NULL, field_type TEXT NOT NULL,
          field_json TEXT NOT NULL, source_schema INTEGER NOT NULL CHECK(source_schema >= 1),
          PRIMARY KEY(record_type,record_id,field_name)
        )
        """.trimIndent(),
        """
        CREATE TABLE listed_security_profile (
          isin TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, symbol TEXT,
          nse_symbol TEXT, bse_symbol TEXT, amfi_code TEXT, category TEXT NOT NULL,
          close_scaled INTEGER NOT NULL DEFAULT 0 CHECK(close_scaled >= 0),
          price_date TEXT NOT NULL, source TEXT NOT NULL, updated_at TEXT NOT NULL
        )
        """.trimIndent(),
        """
        CREATE TABLE egress_log_profile (
          id TEXT PRIMARY KEY NOT NULL, occurred_at TEXT NOT NULL,
          destination TEXT NOT NULL, purpose TEXT NOT NULL,
          request_bytes INTEGER NOT NULL DEFAULT 0 CHECK(request_bytes >= 0),
          response_bytes INTEGER NOT NULL DEFAULT 0 CHECK(response_bytes >= 0),
          outcome TEXT NOT NULL, allow_listed INTEGER NOT NULL DEFAULT 1
            CHECK(allow_listed IN (0,1))
        )
        """.trimIndent(),
        """
        CREATE TABLE gmail_seen_profile (
          message_id TEXT NOT NULL, attachment_id TEXT NOT NULL,
          filename TEXT, owner_id TEXT REFERENCES family_member(id) ON DELETE RESTRICT,
          seen_at TEXT NOT NULL, PRIMARY KEY(message_id,attachment_id)
        )
        """.trimIndent(),
        """
        CREATE TRIGGER deny_tax_rule_profile_update BEFORE UPDATE ON tax_rule_profile
        BEGIN SELECT RAISE(ABORT,'Installed tax policy is immutable'); END
        """.trimIndent(),
        """
        CREATE TRIGGER deny_tax_rule_profile_delete BEFORE DELETE ON tax_rule_profile
        BEGIN SELECT RAISE(ABORT,'Installed tax policy is immutable'); END
        """.trimIndent(),
        """
        CREATE TABLE setting_profile (
          key TEXT PRIMARY KEY NOT NULL, value_json TEXT NOT NULL
        )
        """.trimIndent(),
    )

    /**
     * Original ledger_event stores settlement_date, unit_balance_after, description
     * and entered_by (src/db/schema.ts). They live on ledger_profile so the
     * append-only ledger_event table is not rebuilt. Existing rows keep NULL
     * for the three optional fields and the schema default entered_by 'owner'.
     * A settlement date, when present, must be on or after that event's trade date.
     */
    internal val migrationToV5 = listOf(
        """ALTER TABLE ledger_profile ADD COLUMN settlement_date TEXT
           CHECK(settlement_date IS NULL OR (
             length(settlement_date) = 10 AND
             settlement_date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]' AND
             settlement_date >= '1900-01-01' AND settlement_date <= '2300-12-31'))""",
        "ALTER TABLE ledger_profile ADD COLUMN unit_balance_after INTEGER",
        """ALTER TABLE ledger_profile ADD COLUMN description TEXT
           CHECK(description IS NULL OR length(description) BETWEEN 1 AND 500)""",
        """ALTER TABLE ledger_profile ADD COLUMN entered_by TEXT NOT NULL DEFAULT 'owner'
           CHECK(entered_by = 'owner')""",
        """
        CREATE TRIGGER validate_ledger_profile_settlement BEFORE INSERT ON ledger_profile
        WHEN NEW.settlement_date IS NOT NULL
        BEGIN
          SELECT RAISE(ABORT, 'settlement_date must be on or after the trade date')
          WHERE NOT EXISTS (
            SELECT 1 FROM ledger_event
            WHERE id = NEW.event_id AND NEW.settlement_date >= event_date
          );
        END
        """.trimIndent(),
    )

    // Same immutable v1 DDL used by fresh initialization and v1 migration tests.
    internal val legacyV1Statements: List<String> get() = ddl

    private val ddl = listOf(
        """
        CREATE TABLE family_member (
          id TEXT PRIMARY KEY NOT NULL,
          display_name TEXT NOT NULL CHECK(length(display_name) BETWEEN 1 AND 200),
          relationship TEXT NOT NULL,
          is_primary INTEGER NOT NULL DEFAULT 0 CHECK(is_primary IN (0,1))
        )
        """.trimIndent(),
        "CREATE UNIQUE INDEX one_primary_member ON family_member(is_primary) WHERE is_primary = 1",
        """
        CREATE TABLE custody_account (
          id TEXT PRIMARY KEY NOT NULL,
          owner_id TEXT NOT NULL,
          account_type TEXT NOT NULL,
          identifier_masked TEXT NOT NULL,
          UNIQUE(id, owner_id),
          FOREIGN KEY(owner_id) REFERENCES family_member(id) ON DELETE RESTRICT
        )
        """.trimIndent(),
        """
        CREATE TABLE holding (
          id TEXT PRIMARY KEY NOT NULL,
          owner_id TEXT NOT NULL,
          account_id TEXT,
          display_name TEXT NOT NULL,
          asset_category TEXT NOT NULL,
          quantity_scaled INTEGER NOT NULL DEFAULT 0 CHECK(quantity_scaled >= 0),
          price_scaled INTEGER NOT NULL DEFAULT 0 CHECK(price_scaled >= 0),
          UNIQUE(id, owner_id),
          FOREIGN KEY(owner_id) REFERENCES family_member(id) ON DELETE RESTRICT,
          FOREIGN KEY(account_id, owner_id) REFERENCES custody_account(id, owner_id) ON DELETE RESTRICT
        )
        """.trimIndent(),
        "CREATE INDEX holding_by_owner ON holding(owner_id)",
        """
        CREATE TABLE ledger_event (
          id TEXT PRIMARY KEY NOT NULL,
          owner_id TEXT NOT NULL,
          holding_id TEXT NOT NULL,
          kind TEXT NOT NULL CHECK(kind IN
            ('BUY','SELL','DIVIDEND','INTEREST','OPENING_BALANCE','ADJUSTMENT','CORRECTION')),
          event_date TEXT NOT NULL,
          quantity_delta_scaled INTEGER NOT NULL,
          amount_scaled INTEGER NOT NULL,
          source_fingerprint TEXT,
          created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
          FOREIGN KEY(holding_id, owner_id) REFERENCES holding(id, owner_id) ON DELETE RESTRICT
        )
        """.trimIndent(),
        "CREATE INDEX ledger_by_holding_date ON ledger_event(holding_id, event_date, id)",
        "CREATE UNIQUE INDEX ledger_event_source_uniq ON ledger_event(owner_id, source_fingerprint) WHERE source_fingerprint IS NOT NULL",
        """CREATE TRIGGER deny_ledger_update BEFORE UPDATE ON ledger_event
              BEGIN SELECT RAISE(ABORT, 'ledger_event is append only'); END""",
        """CREATE TRIGGER deny_ledger_delete BEFORE DELETE ON ledger_event
              BEGIN SELECT RAISE(ABORT, 'ledger_event is append only'); END""",
        """
        CREATE TABLE snapshot_import (
          id TEXT PRIMARY KEY NOT NULL,
          owner_id TEXT NOT NULL,
          source_sha256 TEXT NOT NULL UNIQUE,
          as_of TEXT NOT NULL,
          is_complete INTEGER NOT NULL CHECK(is_complete = 1),
          imported_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
          UNIQUE(id, owner_id),
          FOREIGN KEY(owner_id) REFERENCES family_member(id) ON DELETE RESTRICT
        )
        """.trimIndent(),
        """
        CREATE TABLE statement_position (
          snapshot_id TEXT NOT NULL,
          owner_id TEXT NOT NULL,
          holding_id TEXT NOT NULL,
          quantity_scaled INTEGER NOT NULL CHECK(quantity_scaled >= 0),
          value_scaled INTEGER NOT NULL,
          PRIMARY KEY(snapshot_id, holding_id),
          FOREIGN KEY(snapshot_id, owner_id) REFERENCES snapshot_import(id, owner_id) ON DELETE RESTRICT,
          FOREIGN KEY(holding_id, owner_id) REFERENCES holding(id, owner_id) ON DELETE RESTRICT
        )
        """.trimIndent(),
        "CREATE INDEX snapshot_owner ON snapshot_import(owner_id, as_of)",
        """
        CREATE TABLE vault_meta (
          key TEXT PRIMARY KEY NOT NULL,
          value TEXT NOT NULL
        )
        """.trimIndent()
    )
}
