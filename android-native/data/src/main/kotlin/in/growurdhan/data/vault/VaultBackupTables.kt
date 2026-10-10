package `in`.growurdhan.data.vault

import android.database.Cursor
import net.zetetic.database.sqlcipher.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject

/**
 * Whitelisted portable logical backup: no raw database keys, device tokens,
 * sqlite_master data or user-provided SQL. Monetary integers travel as decimal strings.
 */
internal object VaultBackupTables {
    private val columns: LinkedHashMap<String, List<String>> = linkedMapOf(
        "family_member" to listOf("id", "display_name", "relationship", "is_primary"),
        "custody_account" to listOf("id", "owner_id", "account_type", "identifier_masked"),
        "holding" to listOf(
            "id", "owner_id", "account_id", "display_name", "asset_category",
            "quantity_scaled", "price_scaled",
        ),
        "holding_cas_identity" to listOf(
            "holding_id", "owner_id", "account_id", "isin", "folio",
        ),
        "ledger_event" to listOf(
            "id", "owner_id", "holding_id", "kind", "event_date",
            "quantity_delta_scaled", "amount_scaled", "source_fingerprint",
            "financial_kind", "unit_price_scaled", "charges_scaled", "supersedes_event_id",
        ),
        "snapshot_import" to listOf(
            "id", "owner_id", "source_sha256", "as_of", "is_complete",
        ),
        "statement_position" to listOf(
            "snapshot_id", "owner_id", "holding_id", "quantity_scaled", "value_scaled",
        ),
        "vault_meta" to listOf("key", "value"),
        "family_profile" to listOf("owner_id", "entity_type", "pan_masked", "pan_for_statements", "date_of_birth", "date_of_majority", "guardian_id", "color", "notes", "is_active", "created_at", "cas_id", "pran", "gmail_sync", "cas_id_source", "pran_source", "cas_sync_after", "pran_sync_after", "extra_json"),
        "custody_profile" to listOf("account_id", "owner_id", "custodian", "identifier_full", "dp_id", "client_id", "bo_id", "base_currency", "nominee", "opened_on", "closed_on", "created_at", "extra_json"),
        "instrument_profile" to listOf("id", "name", "category", "sub_category", "symbol_or_folio", "isin", "currency", "exposure_node", "l2_node", "wrapper", "allocation_split_json", "fmv_31jan2018_scaled", "amc", "rta", "amfi_code", "advisor", "scheme_type", "ucc", "exchange", "dp_name", "is_user_created", "created_at", "extra_json"),
        "holding_profile" to listOf("holding_id", "owner_id", "instrument_id", "symbol_or_folio", "isin", "sub_category", "average_cost_per_unit_scaled", "cost_unknown", "benchmark_price_scaled", "issue_price_scaled", "coupon_rate_bps", "next_coupon_date", "maturity_date", "interest_rate_bps", "institution_name", "account_number_masked", "monthly_emi_scaled", "emi_status", "remaining_emi_count", "acquired_on", "details_json", "last_price_updated", "is_price_stale", "price_source", "needs_review", "review_reasons_json", "fmv_31jan2018_scaled", "exposure_node", "l2_node", "wrapper", "allocation_split_json", "notes", "location", "tag", "tags_json", "extra_json"),
        "ledger_profile" to listOf("event_id", "owner_id", "holding_id", "holding_name", "account_id", "import_batch_id", "source_ref", "notes", "legacy_created_at", "extra_json"),
        "family_goal" to listOf("id", "owner_id", "name", "target_scaled", "target_date", "priority", "created_at", "extra_json"),
        "holding_goal" to listOf("goal_id", "holding_id", "owner_id"),
        "import_batch_profile" to listOf("id", "owner_id", "filename", "kind", "statement_date", "period_from", "period_to", "investor_name", "investor_pan_masked", "investor_email", "cas_id", "statement_type", "rows_new", "rows_conflict", "rows_duplicate", "committed_at", "source_sha256", "extra_json"),
        "source_document_profile" to listOf("id", "batch_id", "owner_id", "issuer", "filename", "document_kind", "mime_type", "source_sha256", "byte_length", "page_count", "received_at", "extra_json"),
        "statement_snapshot_profile" to listOf("snapshot_id", "owner_id", "issuer", "period_from", "period_to", "import_batch_id", "investor_name", "invested_scaled", "extra_json"),
        "statement_position_profile" to listOf("snapshot_id", "owner_id", "holding_id", "invested_scaled", "source_line_id", "statement_date", "unit_price_scaled", "isin", "folio", "current_value_scaled", "amc", "scheme_type", "source_section", "extra_json"),
        "price_observation_profile" to listOf("id", "instrument_id", "holding_id", "owner_id", "as_of", "price_scaled", "basis", "currency", "source", "recorded_at", "extra_json"),
        "tax_rule_profile" to listOf("id", "effective_from", "fiscal_year", "assessment_year", "rules_json", "source", "published_at", "installed_at"),
        "liability_profile" to listOf("holding_id", "owner_id", "lender", "liability_type", "name", "principal_scaled", "outstanding_scaled", "monthly_emi_scaled", "rate_type", "interest_rate_bps", "remaining_emi_count", "start_date", "end_date", "created_at", "linked_holding_id", "details_json"),
        "legacy_account_identity" to listOf("id", "email", "first_name", "last_name", "pan_masked", "pan_for_statements", "date_of_birth", "password_salt", "password_hash", "password_iterations", "password_changed_at", "security_q1_id", "security_q1_hash", "security_q2_id", "security_q2_hash", "created_at", "extra_json"),
        "legacy_field_extension" to listOf("record_type", "record_id", "owner_id", "field_name", "field_type", "field_json", "source_schema"),
        "listed_security_profile" to listOf("isin", "name", "symbol", "nse_symbol", "bse_symbol", "amfi_code", "category", "close_scaled", "price_date", "source", "updated_at"),
        "egress_log_profile" to listOf("id", "occurred_at", "destination", "purpose", "request_bytes", "response_bytes", "outcome", "allow_listed"),
        "gmail_seen_profile" to listOf("message_id", "attachment_id", "filename", "owner_id", "seen_at"),
        "setting_profile" to listOf("key", "value_json"),
    )
    private val parityTables = setOf(
        "family_profile",
        "custody_profile",
        "instrument_profile",
        "holding_profile",
        "ledger_profile",
        "family_goal",
        "holding_goal",
        "import_batch_profile",
        "source_document_profile",
        "statement_snapshot_profile",
        "statement_position_profile",
        "price_observation_profile",
        "tax_rule_profile",
        "liability_profile",
        "legacy_account_identity",
        "legacy_field_extension",
        "listed_security_profile",
        "egress_log_profile",
        "gmail_seen_profile",
        "setting_profile",
    )
    private val integers = setOf(
        "is_primary", "quantity_scaled", "price_scaled", "quantity_delta_scaled",
        "amount_scaled", "is_complete", "value_scaled", "unit_price_scaled", "charges_scaled",
        "is_active", "is_user_created", "cost_unknown", "is_price_stale",
        "needs_review", "remaining_emi_count", "source_schema",
        "rows_new", "rows_duplicate", "password_iterations", "target_scaled",
        "average_cost_per_unit_scaled", "benchmark_price_scaled",
        "issue_price_scaled", "coupon_rate_bps", "interest_rate_bps",
        "monthly_emi_scaled", "fmv_31jan2018_scaled", "invested_scaled",
        "price_scaled", "principal_scaled", "outstanding_scaled",
        "unit_price_scaled", "current_value_scaled", "close_scaled",
        "request_bytes", "response_bytes", "allow_listed",
        "page_count", "byte_length", "rows_conflict", "gmail_sync",
    )
    private const val MAX_ROWS_PER_TABLE = 100_000
    private val legacyLedgerColumns = columns.getValue("ledger_event").take(8)

    fun export(db: SQLiteDatabase): ByteArray {
        val payload = JSONObject().put("format", 4).put("databaseVersion", VaultSchema.VERSION)
        for ((table, names) in columns) {
            val rows = JSONArray()
            // Ledger corrections are insert-time validated against earlier events.
            // A portable export must preserve physical append order so restore
            // never encounters a CORRECTION before its predecessor.
            val order = if (table == "ledger_event") " ORDER BY rowid" else ""
            db.rawQuery("SELECT ${names.joinToString(",")} FROM $table$order", null).use { cursor ->
                while (cursor.moveToNext()) {
                    check(rows.length() < MAX_ROWS_PER_TABLE) { "Backup has too many financial records." }
                    val row = JSONArray()
                    names.indices.forEach { index ->
                        row.put(
                            when (cursor.getType(index)) {
                                Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                                Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index).toString()
                                Cursor.FIELD_TYPE_STRING -> cursor.getString(index)
                                else -> throw VaultUnavailable("Unsupported backup record field.")
                            }
                        )
                    }
                    rows.put(row)
                }
            }
            payload.put(table, rows)
        }
        return payload.toString().toByteArray(Charsets.UTF_8).also {
            require(it.size <= VaultBackupCodec.MAX_PLAINTEXT) { "Backup export is too large." }
        }
    }

    fun restoreIntoEmpty(db: SQLiteDatabase, plain: ByteArray) {
        require(plain.size <= VaultBackupCodec.MAX_PLAINTEXT) { "Backup is oversized." }
        val payload = JSONObject(plain.toString(Charsets.UTF_8))
        val format = payload.getInt("format")
        val databaseVersion = payload.getInt("databaseVersion")
        // Restore v1/v2/v3 backups into v4 without inventing missing fields.
        // New parity extension tables are present only in format 4 exports.
        require(
            (format == 4 && databaseVersion == VaultSchema.VERSION) ||
                (format == 3 && databaseVersion == 3) ||
                (format == 2 && databaseVersion == 2) ||
                (format == 1 && databaseVersion == 1)
        ) { "Unsupported native backup schema." }
        // Reject any attempt to combine backups or overwrite existing financial books.
        for (table in columns.keys) {
            db.rawQuery("SELECT 1 FROM $table LIMIT 1", null).use {
                check(!it.moveToFirst()) { "Backup restore requires an empty native vault." }
            }
        }
        for ((table, availableNames) in columns) {
            // Old backups predate ISIN/folio mapping. Do not fabricate identities.
            if (format < 3 && table == "holding_cas_identity") continue
            if (format < 4 && table in parityTables) continue
            val names = if (format == 1 && table == "ledger_event") legacyLedgerColumns
                else availableNames
            val rows = payload.getJSONArray(table)
            require(rows.length() <= MAX_ROWS_PER_TABLE) { "Too many backup records." }
            val placeholders = names.joinToString(",") { "?" }
            val statement = "INSERT INTO $table (${names.joinToString(",")}) VALUES ($placeholders)"
            for (i in 0 until rows.length()) {
                val row = rows.getJSONArray(i)
                require(row.length() == names.size) { "Backup column mismatch." }
                val values = arrayOfNulls<Any>(names.size)
                for (j in names.indices) {
                    if (!row.isNull(j)) {
                        val value = row.getString(j)
                        require(value.length <= 1_048_576) { "Backup field is oversized." }
                        values[j] = if (names[j] in integers) value.toLong() else value
                    }
                }
                db.execSQL(statement, values)
            }
        }
    }
}
